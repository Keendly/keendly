package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.model.ExternalUser;
import auth.Authenticator;
import controllers.api.KeendlyHeader;
import dao.UserDao;
import entities.Provider;
import entities.UserEntity;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

public class LoginController extends Controller {

    private Authenticator authenticator = new Authenticator();
    private UserDao userDao = new UserDao();

    public Promise<Result> feedlyCallback(String code, String error){
        if (StringUtils.isNotBlank(error)){
            Logger.error("Error got in feedly callback: {}", error);
            return F.Promise.pure(redirect(routes.WebController.login(error)));
        } else {
            return loginUser(code, Provider.FEEDLY).map(token -> {
                response().setCookie(KeendlyHeader.SESSION_COOKIE.value, token);
                return redirect(routes.WebController.feeds());
            });
        }
    }

    public Promise<String> loginUser(String authrorizationCode, Provider p){
        Adaptor adaptor = Adaptors.getByProvider(p);
        return adaptor.login(authrorizationCode).flatMap(
                token -> adaptor.getUser(token).map(user -> {
                    session("displayName", user.getDisplayName());
                    List<Long> id = new ArrayList<>();
                    JPA.withTransaction(() -> {
                        UserEntity userEntity = findUser(user, p);
                        userEntity.token = token.getRefreshToken();
                        userEntity = JPA.em().merge(userEntity);

                        id.add(userEntity.id); // why so hacky
                    });
                    return authenticator.generate(id.get(0), p, token);
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
