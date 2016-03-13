package com.keendly.controllers;

import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.AdaptorFactory;
import com.keendly.adaptors.model.Credentials;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.auth.Authenticator;
import com.keendly.controllers.api.KeendlyHeader;
import com.keendly.dao.UserDao;
import com.keendly.entities.Provider;
import com.keendly.entities.UserEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

public class LoginController extends Controller {

    private static final Logger.ALogger LOG = Logger.of(LoginController.class);

    private Authenticator authenticator = new Authenticator();
    private UserDao userDao = new UserDao();

    public Promise<Result> feedlyCallback(String code, String error){
        if (StringUtils.isNotBlank(error)){
            Logger.error("Error got in feedly callback: {}", error);
            return F.Promise.pure(redirect(routes.WebController.login(error)));
        } else {
            Credentials credentials = new Credentials();
            credentials.setAuthorizationCode(code);
            return loginUser(credentials, Provider.FEEDLY);
        }
    }

    public Promise<Result> inoReaderLogin(){
        String email = request().body().asFormUrlEncoded().get("email")[0];
        String password = request().body().asFormUrlEncoded().get("password")[0];
        Credentials credentials = new Credentials();
        credentials.setUsername(email);
        credentials.setPassword(password);
        return loginUser(credentials, Provider.INOREADER).recover(f -> {
            LOG.error("Error loggin in with inoReader", f);
            return redirect(routes.WebController.login("Login error"));
        });
    }

    public Promise<Result> oldReaderLogin(){
        String email = request().body().asFormUrlEncoded().get("email")[0];
        String password = request().body().asFormUrlEncoded().get("password")[0];
        Credentials credentials = new Credentials();
        credentials.setUsername(email);
        credentials.setPassword(password);
        return loginUser(credentials, Provider.OLDREADER).recover(f -> {
            LOG.error("Error loggin in with theOldReader", f);
            return redirect(routes.WebController.login("Login error"));
        });
    }

    public Promise<Result> loginUser(Credentials credentials, Provider p){
        Adaptor adaptor = AdaptorFactory.getInstance(p);
        return adaptor.login(credentials).flatMap(
                token -> adaptor.getUser().map(user -> {
                    session("displayName", user.getDisplayName());
                    List<Long> id = new ArrayList<>();
                    JPA.withTransaction(() -> {
                        UserEntity userEntity = findUser(user, p);
                        userEntity.token = token.getRefreshToken();
                        userEntity = JPA.em().merge(userEntity);
                        id.add(userEntity.id); // why so hacky
                    });
                    String t = authenticator.generate(id.get(0), p, token);
                    response().setCookie(KeendlyHeader.SESSION_COOKIE.value, t);
                    return redirect(routes.WebController.feeds());
                })
        );
    }

    protected UserEntity findUser(ExternalUser externalUser, Provider provider){
        UserEntity entity = userDao.findByProviderId(externalUser.getId(), provider);
        if (entity == null){
            entity = userDao.createUser(externalUser.getId(), provider, externalUser.getUserName());
        }
        return entity;
    }
}