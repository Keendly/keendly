package com.keendly.controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.controllers.api.error.Error;
import com.keendly.dao.SubscriptionDao;
import com.keendly.entities.SubscriptionEntity;
import com.keendly.entities.SubscriptionFrequency;
import com.keendly.entities.SubscriptionItemEntity;
import com.keendly.entities.UserEntity;
import com.keendly.model.DeliveryItem;
import com.keendly.model.Subscription;
import com.keendly.model.User;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import sun.util.calendar.ZoneInfo;

import java.util.*;

@With(SecuredAction.class)
public class SubscriptionController extends AbstractController<Subscription> {

    private static final play.Logger.ALogger LOG = play.Logger.of(SubscriptionController.class);


    private SubscriptionDao subscriptionDao = new SubscriptionDao();

    public Promise<Result> createSubscription() {
        // HACK WARNING
        StringBuilder deliveryEmail = new StringBuilder();
        StringBuilder deliverySender = new StringBuilder();
        StringBuilder userId = new StringBuilder();
        JPA.withTransaction(() -> {
            UserEntity userEntity = new UserController().lookupUser("self");
            if (userEntity.deliveryEmail != null){
                if (userEntity.deliveryEmail != null){
                    deliveryEmail.append(userEntity.deliveryEmail);
                }
                if (userEntity.deliverySender != null){
                    deliverySender.append(userEntity.deliverySender);
                }
                userId.append(userEntity.id);
            }
        });
        if (deliveryEmail.toString().isEmpty()){
            return Promise.pure(badRequest(toJson(Error.DELIVERY_EMAIL_NOT_CONFIGURED)));
        }

        if (deliverySender.toString().isEmpty()){
            return Promise.pure(badRequest(toJson(Error.DELIVERY_SENDER_NOT_SET)));
        }

        Subscription subscription = fromRequest();

        if (subscription.feeds.size() > DeliveryController.MAX_FEEDS_IN_DELIVERY){
            return Promise.pure(badRequest(toJson(Error.TOO_MANY_ITEMS, DeliveryController.MAX_FEEDS_IN_DELIVERY)));
        }
        TimeZone timezone = ZoneInfo.getTimeZone(subscription.timezone);

        SubscriptionEntity entity = new SubscriptionEntity();
        entity.active = Boolean.TRUE;
        entity.frequency = SubscriptionFrequency.DAILY;
        entity.time = subscription.time;
        entity.timeZone = timezone.toZoneId().getId();
        entity.items = new ArrayList<>();
        entity.user = getDummyUserEntity();
        entity.deleted = false;

        for (DeliveryItem feed : subscription.feeds){
            SubscriptionItemEntity item = new SubscriptionItemEntity();
            item.feedId = feed.feedId;
            item.fullArticle = feed.fullArticle;
            item.markAsRead = feed.markAsRead;
            item.withImages = feed.includeImages;
            item.subscription = entity;
            item.title = feed.title;
            entity.items.add(item);
        }

        JPA.withTransaction(() -> subscriptionDao.createSubscription(entity));
        return F.Promise.pure(created());
    }

    private JsonNode toJson(Error error, Object... msgParams){
        Map<String, String> map = new HashMap<>();
        map.put("code", error.name());
        map.put("description", String.format(error.getMessage(), msgParams));
        return Json.toJson(map);
    }

    public Result getSubscription(String id) throws Throwable {
        return JPA.withTransaction(() -> {
            SubscriptionEntity s = subscriptionDao.getSubscription(id);
            if (s == null){
                return notFound();
            }
            return ok(Json.toJson(map(s, false)));
        });
    }

    public Result getSubscriptionsToDeliver() throws Throwable{
        return JPA.withTransaction(() -> {
            List<SubscriptionEntity> s = subscriptionDao.getDailySubscriptionsToDeliver();
            if (s == null){
                return notFound();
            }
            System.out.println(s.size());
            return ok(Json.toJson(map(s, true)));
        });
    }

    public Promise<Result> getSubscriptions(int page, int pageSize) {
        List<Subscription> deliveries = new ArrayList<>();
        JPA.withTransaction(() -> {
            List<SubscriptionEntity> entities = subscriptionDao.getSubscriptions(getUserEntity(), page, pageSize);
            deliveries.addAll(map(entities, false));
        });

        return Promise.pure(ok(Json.toJson(deliveries)));
    }

    private List<Subscription> map(List<SubscriptionEntity> entities, boolean withUser){
        List<Subscription> result = new ArrayList<>();
        for (SubscriptionEntity entity : entities){
            result.add(map(entity, withUser));
        }
        return result;
    }

    private Subscription map(SubscriptionEntity entity, boolean withUser){
        Subscription subscription = new Subscription();
        subscription.id = entity.id;
        subscription.time = entity.time;
        subscription.timezone = entity.timeZone;
        subscription.feeds = new ArrayList<>();
        subscription.frequency = entity.frequency.name();
        subscription.created = entity.created;
        subscription.active = entity.active;
        for (SubscriptionItemEntity item : entity.items){
            DeliveryItem itemResponse = new DeliveryItem();
            itemResponse.feedId = item.feedId;
            itemResponse.fullArticle = item.fullArticle;
            itemResponse.markAsRead = item.markAsRead;
            itemResponse.includeImages = item.withImages;
            itemResponse.title = item.title;
            subscription.feeds.add(itemResponse);
        }
        if (withUser){
            User user = new User();
            user.id = entity.user.id;
            user.email = entity.user.email;
            user.deliveryEmail = entity.user.deliveryEmail;
            user.provider = entity.user.provider;
            subscription.user = user;
        }
        return subscription;
    }

    public Result deleteSubscription(String id) throws Throwable {
        return JPA.withTransaction(() -> {
            boolean success = subscriptionDao.deleteSubscription(id);
            if (!success){
                return notFound();
            }
            return ok();
        });
    }

    public Result updateSubscription(String id) {
        // only updating acive field supported!!!
        Subscription subscription = fromRequest();
        try {
            return JPA.withTransaction(() -> {
                SubscriptionEntity subscriptionEntity = subscriptionDao.getSubscription(id);
                subscriptionEntity.active = subscription.active;
                subscriptionDao.updateSubscription(subscriptionEntity);
                return ok();
            });
        } catch (Throwable throwable) {
            LOG.error("Error updating subscription " + id, throwable);
            return internalServerError();
        }
    }
}
