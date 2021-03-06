package com.keendly.controllers.api;

import com.keendly.auth.Authenticator;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

public class SecuredAction extends Action.Simple {

    private static final play.Logger.ALogger LOG = play.Logger.of(SecuredAction.class);

    private Authenticator authenticator = new Authenticator();

    public Promise<Result> call(Context ctx) throws Throwable {
        try {
            String token = findToken(ctx);
            if (token == null){
                LOG.warn("Token not found");
                return Promise.pure(unauthorized());
            }
            String userId = authenticator.parse(token);
            ctx.args.put("authenticatedUser", userId);
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
            LOG.warn("Exception authenticating", e);
            return Promise.pure(unauthorized());
        }
    }

    private String findToken(Context ctx){
        String token = ctx.request().getHeader(KeendlyHeader.AUTHORIZATION.value);
        if (token != null && token.contains(" ")){
            token = token.split(" ")[1];
        }
        if (token == null){
            Http.Cookie cookie = ctx.request().cookie(KeendlyHeader.SESSION_COOKIE.value);
            if (cookie != null){
                token = cookie.value();
            }
        }
        return token;
    }
}
