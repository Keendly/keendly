package com.keendly.controllers.api;

import com.keendly.auth.AuthToken;
import com.keendly.auth.Authenticator;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

import java.util.logging.Logger;

public class SecuredAction extends Action.Simple {

    private static Logger LOG = Logger.getLogger(SecuredAction.class.getCanonicalName());

    private Authenticator authenticator = new Authenticator();

    public Promise<Result> call(Context ctx) throws Throwable {
        try {
            AuthToken token = authenticator.parse(findToken(ctx));
            ctx.args.put("token", token);
            return delegate.call(ctx);
//            return adaptor.getUser(externalToken).flatMap(user -> {
//                ctx.args.put("user", user);
//                ctx.args.put("token", token);
//                if (!externalToken.getAccessToken().equals(token.accessToken)){
//                    // access token refreshed
//                    String newToken = authenticator.generate(token.userId, token.provider, externalToken);
//                    ctx.response().setHeader(KeendlyHeader.NEW_TOKEN.value, newToken);
//                }
//                return delegate.call(ctx);
//            }).recover(error -> {
//                error.printStackTrace();
//                return internalServerError();
//            });
        } catch (Exception e){
            LOG.severe("Exception authenticating");
            e.printStackTrace();
            return Promise.pure(unauthorized());
        }
    }

    private String findToken(Context ctx){
        String token = ctx.request().getHeader(KeendlyHeader.AUTHORIZATION.value);
        if (token == null){
            token = ctx.request().cookie(KeendlyHeader.SESSION_COOKIE.value).value();
        }
        return token;
    }
}
