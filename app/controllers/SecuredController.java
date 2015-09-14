package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.home;

public class SecuredController extends Controller {

    public Result home(){
        return ok(home.render(session()));
    }
}
