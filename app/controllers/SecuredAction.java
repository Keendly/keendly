package controllers;

import entities.User;
import play.Logger;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.SessionUtils;

public class SecuredAction extends Action.Simple {
    public Promise<Result> call(Http.Context ctx) throws Throwable {
        try {
            User user = SessionUtils.getUser(ctx.session());
            if (user != null) {
                return delegate.call(ctx);
            }
        } catch (Exception e){
            Logger.warn("Error retrieving user from context", e);
        }
        ctx.flash().put(Constants.FLASH_ERROR, Messages.get("login.notLogged"));
        return Promise.pure(delegate.redirect(routes.MainController.login()));
    }
}
