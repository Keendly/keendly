package com.keendly.controllers.api;

import com.keendly.dao.SubscriptionDao;
import com.keendly.entities.SubscriptionEntity;
import com.keendly.entities.SubscriptionFrequency;
import com.keendly.entities.SubscriptionItemEntity;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@With(SecuredAction.class)
public class SubscriptionController extends AbstractController<Subscription> {

    private SubscriptionDao subscriptionDao = new SubscriptionDao();

    public Promise<Result> createSubscription() {
        Subscription subscription = fromRequest();
        TimeZone timezone = ZoneInfo.getTimeZone(subscription.timezone);
        LocalTime time = LocalTime.parse(subscription.time, dateTimeFormatter());

        SubscriptionEntity entity = new SubscriptionEntity();
        entity.active = Boolean.TRUE;
        entity.frequency = SubscriptionFrequency.DAILY;
        entity.time = time.toString();
        entity.timeZone = timezone.toZoneId().getId();
        entity.items = new ArrayList<>();
        entity.user = getDummyUserEntity();

        for (DeliveryItem feed : subscription.feeds){
            SubscriptionItemEntity item = new SubscriptionItemEntity();
            item.feedId = feed.feedId;
            item.fullArticle = feed.fullArticle;
            item.markAsRead = feed.markAsRead;
            item.withImages = feed.includeImages;
            item.subscription = entity;
            entity.items.add(item);
        }

        JPA.withTransaction(() -> subscriptionDao.createSubscription(entity));
        return F.Promise.pure(created());
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
        subscription.time = LocalTime.parse(entity.time).format(dateTimeFormatter());
        subscription.timezone = entity.timeZone;
        subscription.feeds = new ArrayList<>();
        subscription.frequency = entity.frequency.name();
        for (SubscriptionItemEntity item : entity.items){
            DeliveryItem itemResponse = new DeliveryItem();
            itemResponse.feedId = item.feedId;
            itemResponse.fullArticle = item.fullArticle;
            itemResponse.markAsRead = item.markAsRead;
            itemResponse.includeImages = item.withImages;
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

    public Result updateSubscription(String id){
        return ok();
    }
}
