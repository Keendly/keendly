package com.keendly.controllers.api;


import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.controllers.api.error.Error;
import com.keendly.dao.UserDao;
import com.keendly.entities.UserEntity;
import com.keendly.entities.UserNotificationEntity;
import com.keendly.model.User;
import com.keendly.model.UserNotification;
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
                User user = fromEntity(userEntity);
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
        List<User> users = new ArrayList<>();
        if (user.deliveryEmail != null){
            if (!validateDeliveryEmail(user.deliveryEmail)){
                return badRequest(toJson(Error.WRONG_EMAIL, StringUtils.join(", ", ALLOWED_DOMAINS)));
            }
            user.deliverySender = generateSenderEmail(user.deliveryEmail);
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
            if (user.deliverySender != null){
                userEntity.deliverySender = user.deliverySender;
            }
            UserEntity updated = userDao.updateUser(userEntity);
            users.add(fromEntity(updated));
        });
        return ok(Json.toJson(users.get(0)));
    }

    private User fromEntity(UserEntity userEntity){
        User user = new User();
        user.id = userEntity.id;
        user.email = userEntity.email;
        user.deliveryEmail = userEntity.deliveryEmail;
        user.deliverySender = userEntity.deliverySender;
        user.provider = userEntity.provider;
        user.providerId = userEntity.providerId;
        return user;
    }

    private JsonNode toJson(Error error, Object... msgParams){
        Map<String, String> map = new HashMap<>();
        map.put("code", error.name());
        map.put("description", String.format(error.getMessage(), msgParams));
        return Json.toJson(map);
    }

    private boolean validateDeliveryEmail(String email){
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

    private String generateSenderEmail(String deliveryEmail){
        String[] split = deliveryEmail.split("\\@");
        return split[0] + "@keendly.com";
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

    @With(SecuredAction.class)
    public Result getUserNotifications(String id){
        long userId = SELF.equals(id) ? getAuthenticatedUserId() : Long.parseLong(id);
        List<UserNotification> notifications = new ArrayList<>();
        if (SELF.equals(id)){
            id = getAuthenticatedUserId() + "";
        }
        JPA.withTransaction(() -> {
            List<UserNotificationEntity> notificationEntities = userDao.getUserNotificaitons(userId);

            for (UserNotificationEntity entity : notificationEntities){
                UserNotification notification = new UserNotification();
                notification.sendDate = entity.sendDate;
                notification.type = entity.type;
                notifications.add(notification);
            }
        });
        return ok(Json.toJson(notifications));
    }

    @With(SecuredAction.class)
    public Result createUserNotification(String id){
        JsonNode json = request().body().asJson();
        UserNotification notification = Json.fromJson(json, UserNotification.class);

        JPA.withTransaction(() -> {
            UserEntity userEntity = getDummyUserEntity();
            if (!id.equals(SELF)){
                userEntity = new UserEntity();
                userEntity.id = Long.parseLong(id);
            }

            UserNotificationEntity entity = new UserNotificationEntity();
            entity.user = userEntity;
            entity.sendDate = notification.sendDate;
            entity.type = notification.type;
            userDao.createUserNotification(entity);
        });

        return ok();
    }
}
