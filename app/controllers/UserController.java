package controllers;


import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.model.ExternalUser;
import auth.Authenticator;
import dao.UserDao;
import entities.Provider;
import entities.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import model.User;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UserController extends AbstractController<User> {

    private final static String SELF = "self";

    private UserDao userDao = new UserDao();
    private Authenticator authenticator = new Authenticator();

    public F.Promise<Result> loginUser(String authrorizationCode, String provider){
        Provider p = Provider.valueOf(provider);
        Adaptor adaptor = Adaptors.getByProvider(p);
        return adaptor.login(authrorizationCode).flatMap(
                token -> adaptor.getUser(token).map(user -> {

                    List<Long> id = new ArrayList<>();
                    JPA.withTransaction(() -> {
                        UserEntity userEntity = findUser(user, p);
                        userEntity.token = token.getRefreshToken();
                        userEntity = JPA.em().merge(userEntity);

                        id.add(userEntity.id); // why so hacky
                    });
                    String authToken = authenticator.generate(id.get(0), p, token);
                    return ok(authToken);
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

    @With(SecuredAction.class)
    public Result getUser(String id)throws Exception{
        Key key = MacProvider.generateKey();
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println(encodedKey);
        String s = Jwts.builder().setSubject("Joe").signWith(SignatureAlgorithm.HS512, key).compact();
        System.out.println(s);
        List<User> users = new ArrayList<>();
        JPA.withTransaction(() -> {
            UserEntity userEntity = lookupUser(id);

            if (userEntity != null){
                // TODO mapper
                User user = new User();
                user.id = userEntity.id;
                user.email = userEntity.email;
                user.deliveryEmail = userEntity.deliveryEmail;
                user.provider = userEntity.provider;
                user.providerId = userEntity.providerId;
                users.add(user);
            }
        });
        if (users.isEmpty()){
            return notFound();
        } else {
            return ok(Json.toJson(users.get(0)));
        }
    }

    @With(SecuredAction.class)
    public Result updateUser(String id) throws Throwable{
        User user = fromRequest();
        JPA.withTransaction(() -> {
            UserEntity userEntity = lookupUser(id);
            // TODO mapper again
            if (user.deliveryEmail != null){
                userEntity.deliveryEmail = user.deliveryEmail;
            }
            if (user.email != null){
                userEntity.email = user.email;
            }
            userDao.updateUser(userEntity);
        });
        return ok();
    }

    private UserEntity lookupUser(String id){
        // TODO fixit after auth refactor
        UserEntity userEntity = null;
        if (SELF.equals(id)){
            userEntity = getUserEntity();
        } else {
            userEntity = userDao.findById(Long.parseLong(id));
        }
        return userEntity;
    }
}
