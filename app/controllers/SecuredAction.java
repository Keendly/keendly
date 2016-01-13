package controllers;

import adaptors.Adaptor;
import auth.Tokens;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

import java.util.logging.Logger;

import static controllers.RequestUtils.getAdaptor;
import static controllers.RequestUtils.getTokens;

public class SecuredAction extends Action.Simple {

    private static Logger LOG = Logger.getLogger(SecuredAction.class.getCanonicalName());

    public Promise<Result> call(Context ctx) throws Throwable {
        try {
            Adaptor adaptor = getAdaptor(ctx.request());
            Tokens tokens = getTokens(ctx.request());

            return adaptor.getUser(tokens).flatMap(user -> {
                String providerId = ctx.request().getHeader(KeendlyHeader.PROVIDER_ID.value);
                if (user.getId().equals(providerId)){
                    ctx.args.put("user", user);
                    return delegate.call(ctx);
                } else {
                    LOG.severe(String.format("Logged user ids do not match! Got: %s, expected: %s. Provider: %s",
                            user.getId(), providerId, ctx.request().getHeader(KeendlyHeader.PROVIDER.value)));
                    return Promise.pure(unauthorized());
                }
            }).recover(error -> {
                error.printStackTrace();
                return internalServerError();
            });
        } catch (Exception e){
            LOG.severe("Exception authenticating");
            e.printStackTrace();
            return Promise.pure(unauthorized());
        }
    }
}
