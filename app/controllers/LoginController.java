package controllers;

import adaptors.Adaptor;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class LoginController extends Controller {

    public Promise<Result> login(String code){
        Adaptor adaptor = RequestUtils.getAdaptor(ctx().request());
        return adaptor.login(code).map(tokens -> {
            return ok(Json.toJson(tokens));
        });
    }
}
