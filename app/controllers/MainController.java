package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.Provider;
import adaptors.auth.Tokens;
import models.Person;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.login;

import java.util.List;

import static play.libs.Json.*;

public class MainController extends Controller {

    private static final String SESSION_AUTH = "auth";
    private static final String SESSION_USER = "user";

    public Result index() {
        /*  HACK for feedly sandbox - doesn't support custom callback urls
            redirect to feedly callback if code or error query param is present
         */
        String code = getQueryParam("code");
        String error = getQueryParam("error");
        if (StringUtils.isNotEmpty(code) || StringUtils.isNotEmpty(error)){
            return redirect(routes.MainController.feedlyCallback(code, error));
        }
        return redirect(routes.MainController.login());
    }

    public Promise<Result> feedlyCallback(String code, String error){
        if (StringUtils.isNotBlank(error)){
            // TODO log error to sentry
            flash("error", "couldn't login"); // TODO translation
            return Promise.pure(redirect(routes.MainController.login()));
        } else {
            Promise<Tokens> tokensFuture = Adaptors.getByProvider(Provider.FEEDLY).getTokens(code);
            return tokensFuture.map(tokens -> {
                tokens.setProvider(Provider.FEEDLY);
                session(SESSION_AUTH, toJson(tokens).toString());
                return redirect(routes.SecuredController.home());
            }).recover(exception -> {
                flash("error", "couldn't exchange code for token, redirecting to login"); // TODO translation
                return redirect(routes.MainController.login());
            });
        }
    }

    private String getQueryParam(String key){
        String value[] = request().queryString().get(key);
        if (value != null && value.length > 0){
            return value[0];
        }
        return null;
    }

    public Promise<Result> login(){
        String tokensString = session(SESSION_AUTH);
        if (StringUtils.isNotEmpty(tokensString)){
            Tokens tokens = fromJson(parse(tokensString), Tokens.class);
            Adaptor adaptor = Adaptors.getByProvider(tokens.getProvider());
            return adaptor.getUser(tokens).map(user -> {
                session(SESSION_USER, toJson(user).toString());
                // this is to store new access token, in case it got refreshed,
                // TODO detect when changed and set only if then
                session(SESSION_AUTH, toJson(tokens).toString());
                return redirect(routes.SecuredController.home());
            }).recover(exception -> {
                session(SESSION_AUTH, StringUtils.EMPTY);// clear session auth since it didn't work
                flash("error", "couldn't get user profile, try again"); // TODO translation
                return ok(login.render(flash()));
            });
        }
        return Promise.pure(ok(login.render(flash())));
    }

    @Transactional
    public Result addPerson() {
        Person person = Form.form(Person.class).bindFromRequest().get();
        JPA.em().persist(person);
        return redirect(routes.MainController.index());
    }

    @Transactional(readOnly = true)
    public Result getPersons() {
        List<Person> persons = (List<Person>) JPA.em().createQuery("select p from Person p").getResultList();
        return ok(toJson(persons));
    }
}
