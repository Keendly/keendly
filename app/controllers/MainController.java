package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.Provider;
import adaptors.auth.Tokens;
import models.Person;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import utils.SessionUtils;
import views.html.login;

import java.util.List;

import static play.libs.Json.*;
import static controllers.Constants.*;

public class MainController extends Controller {

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
            Logger.error("Error got in feedly callback: {}", error);
            flash(FLASH_ERROR, Messages.get("login.error"));
            return Promise.pure(redirect(routes.MainController.login()));
        } else {
            Promise<Tokens> tokensFuture =
                    Adaptors.getByProvider(Provider.FEEDLY).getTokens(code);
            return tokensFuture.map(tokens -> {
                session(SESSION_PROVIDER, Provider.FEEDLY.name());
                session(SESSION_AUTH, toJson(tokens).toString());
                return redirect(routes.MainController.login());
            }).recover(exception -> {
                Logger.error("Error exchanging code for refresh token", exception);
                flash(FLASH_ERROR, Messages.get("login.error"));
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
        Tokens tokens = SessionUtils.findTokens(session());
        if (tokens != null){
            String accessToken = tokens.getAccessToken();
            Adaptor adaptor = SessionUtils.findAdaptor(session());
            return adaptor.getUser(tokens).map(user -> {
                session(SESSION_USER, toJson(user).toString());
                // if access token got changed (refreshed), set it in session cookie
                if (tokens.getAccessToken().equals(accessToken)){
                    session(SESSION_AUTH, toJson(tokens).toString());
                }
                return redirect(routes.SecuredController.home());
            }).recover(exception -> {
                Logger.error("Error getting user profile", exception);
                // clear auth info in session, since they didn't work
                session(SESSION_AUTH, StringUtils.EMPTY);
                flash(FLASH_ERROR, Messages.get("login.error"));
                return ok(login.render(flash()));
            });
        }
        return Promise.pure(ok(login.render(flash())));
    }

    public Result logout(){
        session().clear();
        return redirect(routes.MainController.index());
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
