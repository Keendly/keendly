package controllers;

import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public class SecuredAction extends Action.Simple {
    public Promise<Result> call(Http.Context ctx) throws Throwable {
        String username = ctx.session().get(Constants.SESSION_USER);
        if (username != null){
            return delegate.call(ctx);
        } else {
            ctx.flash().put(Constants.FLASH_ERROR, Messages.get("login.notLogged"));
            return Promise.pure(delegate.redirect(routes.MainController.login()));
        }
    }
}
