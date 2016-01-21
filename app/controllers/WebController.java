package controllers;

import org.apache.commons.lang3.StringUtils;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import views.html.login;
import views.html.feeds;

public class WebController extends Controller {

    public Result index() {
        /*  HACK for feedly sandbox - doesn't support custom callback urls
            redirect to feedly callback if code or error query param is present
         */
        String code = getQueryParam("code");
        String error = getQueryParam("error");
        if (StringUtils.isNotEmpty(code) || StringUtils.isNotEmpty(error)){
            return redirect(routes.LoginController.feedlyCallback(code, error));
        }
        return redirect(routes.WebController.feeds());
    }

    public Result login(String error){
        return ok(login.render(error));
    }

    public Result feeds(){
        return ok(feeds.render());
    }

    public Result deliveries(){
        return ok(index.render("History | Keendly", "deliveries.js"));
    }

    private String getQueryParam(String key){
        String value[] = request().queryString().get(key);
        if (value != null && value.length > 0){
            return value[0];
        }
        return null;
    }
}
