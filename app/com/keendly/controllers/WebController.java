package com.keendly.controllers;

import com.keendly.views.html.problem;
import play.mvc.Controller;
import play.mvc.Result;

public class WebController extends Controller {

    public Result error(String ignore) {
        return ok(problem.render());
    }

//    public Result index() {
//        /*  HACK for feedly sandbox - doesn't support custom callback urls
//            redirect to feedly callback if code or error query param is present
//         */
//        String code = getQueryParam("code");
//        String error = getQueryParam("error");
//        if (StringUtils.isNotEmpty(code) || StringUtils.isNotEmpty(error)){
//            return redirect(routes.LoginController.feedlyCallback(code, error));
//        }
//        return redirect(routes.WebController.feeds());
//    }
//
//    public Result login(String error){
//        return ok(login.render(error));
//    }
//
//    public Result feeds(){
//        return ok(feeds.render());
//    }
//
//    public Result deliveries(){
//        return ok(index.render("History | Keendly", "deliveries.js"));
//    }
//
//    public Result subscriptions(){
//        return ok(index.render("Subscriptions | Keendly", "subscriptions.js"));
//    }
//
//    public Result user(){
//        return ok(index.render("Settings | Keendly", "user.js"));
//    }
//
//    public Result logout(){
//        response().discardCookie(KeendlyHeader.SESSION_COOKIE.value);
//        return redirect(routes.WebController.login(null));
//    }

    private String getQueryParam(String key){
        String value[] = request().queryString().get(key);
        if (value != null && value.length > 0){
            return value[0];
        }
        return null;
    }
}
