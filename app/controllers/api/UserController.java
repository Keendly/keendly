package controllers.api;


import dao.UserDao;
import entities.UserEntity;
import model.User;
import play.db.jpa.JPA;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;

public class UserController extends AbstractController<User> {

    private final static String SELF = "self";

    private UserDao userDao = new UserDao();

    @With(SecuredAction.class)
    public Result getUser(String id)throws Exception{
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
        Long userId;
        if (SELF.equals(id)){
            userId = getUserEntity().id;
        } else {
            userId = Long.parseLong(id);
        }
        return userDao.findById(userId);
    }
}