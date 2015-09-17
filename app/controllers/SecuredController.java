package controllers;

import adaptors.Adaptor;
import adaptors.auth.Tokens;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.SessionUtils;
import views.html.home;

@With(SecuredAction.class)
public class SecuredController extends Controller {

    public Promise<Result> home(){
        Tokens tokens = SessionUtils.findTokens(session());
        Adaptor adaptor = SessionUtils.findAdaptor(session());
        return adaptor.getSubscriptions(tokens).map(subscriptions ->

            ok(home.render(subscriptions, session()))
        );

    }
}
