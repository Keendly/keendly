package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.auth.Tokens;
import dao.UserDao;
import models.Provider;
import models.User;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import utils.SessionUtils;
import views.html.login;

import static controllers.Constants.*;
import static play.libs.Json.toJson;

public class MainController extends Controller {

    private UserDao userDao = new UserDao();

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

    @Transactional
    public Promise<Result> login(){
        Tokens tokens = SessionUtils.findTokens(session());
        if (tokens != null){
            String accessToken = tokens.getAccessToken();
            Adaptor adaptor = SessionUtils.findAdaptor(session());
            return adaptor.getUser(tokens).map(user -> {
                JPA.withTransaction(() -> {
                    User existingUser = userDao.findByProviderId(user.getId(), tokens.getProvider());
                    if (existingUser == null){
                        existingUser = userDao.createUser(user.getId(), tokens.getProvider(), user.getUserName());
                    }
                    session(SESSION_USER, toJson(existingUser).toString());
                });
                // if access token got changed (refreshed), set it in session cookie
                if (!tokens.getAccessToken().equals(accessToken)){
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
}
