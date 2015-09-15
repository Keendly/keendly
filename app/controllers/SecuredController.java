package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.home;

@With(SecuredAction.class)
public class SecuredController extends Controller {

    public Result home(){
        return ok(home.render(session()));
    }
}
