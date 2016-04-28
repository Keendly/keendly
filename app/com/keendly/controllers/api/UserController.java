package com.keendly.controllers.api;


import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.controllers.api.error.Error;
import com.keendly.dao.UserDao;
import com.keendly.entities.UserEntity;
import com.keendly.model.User;
import play.db.jpa.JPA;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.util.*;

public class UserController extends AbstractController<User> {

    private final static String SELF = "self";

    private static final String[] ALLOWED_DOMAINS = {"kindle.com", "free.kindle.com", "kindle.cn", "pbsync.com"};

    private UserDao userDao = new UserDao();

    @With(SecuredAction.class)
    public Result getUser(String id) throws Exception{
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
        if (user.deliveryEmail != null){
            if (!validateEmail(user.deliveryEmail)){
                return badRequest(toJson(Error.WRONG_EMAIL, StringUtils.join(", ", ALLOWED_DOMAINS)));
            }
        }
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

    private JsonNode toJson(Error error, Object... msgParams){
        Map<String, String> map = new HashMap<>();
        map.put("code", error.name());
        map.put("description", String.format(error.getMessage(), msgParams));
        return Json.toJson(map);
    }

    private boolean validateEmail(String email){
        String[] split = email.split("\\@");
        if (split.length != 2){
            return false;
        }
        boolean valid = false;
        for (String allowedDomain : ALLOWED_DOMAINS){
            if (split[1].equals(allowedDomain)){
                valid = true;
                break;
            }
        }
        return valid;
    }

    /**
     * HACK for deliveryEmail check in DeliveryController, should not be public probably
     */
    public UserEntity lookupUser(String id){
        Long userId;
        if (SELF.equals(id)){
            userId = getAuthenticatedUserId();
        } else {
            userId = Long.parseLong(id);
        }
        return userDao.findById(userId);
    }
}
