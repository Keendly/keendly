package controllers;

import adaptors.auth.Tokens;
import adaptors.feedly.FeedlyAdaptor;
import models.Person;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import utils.PlayUtils;
import views.html.home;
import views.html.login;

import java.util.List;

import static play.libs.Json.toJson;

public class Application extends Controller {

    public Result index() {
        String code = getQueryParam("code");
        String error = getQueryParam("error");
        if (StringUtils.isNotEmpty(code) || StringUtils.isNotEmpty(error)){
            return handleFeedlyLogin(code, error);
        }
        return redirect(routes.Application.login());
    }

    private Result handleFeedlyLogin(String code, String error){
        if (StringUtils.isNotBlank(error)){
            // flash error and redirect to login
            return status(500);
        } else {
            Tokens tokens = new FeedlyAdaptor().getTokens(code);
            session("tokens", toJson(tokens).toString());
            return redirect(routes.Application.home());
        }
    }

    private String getQueryParam(String key){
        String value[] = request().queryString().get(key);
        if (value != null && value.length > 0){
            return value[0];
        }
        return null;
    }

    public Result home(){
        return ok(home.render());
    }

    public Result login(){
        return ok(login.render());
    }

    public Result loginFeedly(){
        return redirect(PlayUtils.configParam("feedly.url") +
                "/auth/auth?response_type=code&client_id=" +
                PlayUtils.configParam("feedly.client_id") +
                "&redirect_uri=" +
                PlayUtils.configParam("feedly.redirect_uri") +
                "&scope=https://cloud.feedly.com/subscriptions");
     }

    @Transactional
    public Result addPerson() {
        Person person = Form.form(Person.class).bindFromRequest().get();
        JPA.em().persist(person);
        return redirect(routes.Application.index());
    }

    @Transactional(readOnly = true)
    public Result getPersons() {
        List<Person> persons = (List<Person>) JPA.em().createQuery("select p from Person p").getResultList();
        return ok(toJson(persons));
    }
}
