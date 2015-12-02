package controllers;

import adaptors.Adaptor;
import adaptors.model.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.model.DeliveryModel;
import controllers.model.Feed;
import controllers.model.SubscriptionModel;
import dao.SubscriptionDao;
import entities.Subscription;
import entities.SubscriptionFrequency;
import entities.SubscriptionItem;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import sun.util.calendar.ZoneInfo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.TimeZone;

@With(SecuredAction.class)
public class RestController extends AbstractController {

    private SubscriptionDao subscriptionDao = new SubscriptionDao();

    public F.Promise<Result> deliver() {
        JsonNode json = request().body().asJson();
        System.out.println(json);
        DeliveryModel request = Json.fromJson(json, DeliveryModel.class);

        // get articles from adaptor, call lautus and send to S3
        Tokens tokens = findTokens();
        Adaptor adaptor = findAdaptor();

        return F.Promise.pure(ok());
    }

    public F.Promise<Result> schedule() {
        JsonNode json = request().body().asJson();
        SubscriptionModel request = Json.fromJson(json, SubscriptionModel.class);

        TimeZone timezone = ZoneInfo.getTimeZone(request.timezone);
        LocalTime time = LocalTime.parse(request.time, dateTimeFormatter());
        Subscription subscription = new Subscription();
        subscription.active = Boolean.TRUE;
        subscription.frequency = SubscriptionFrequency.DAILY;
        subscription.time = time.toString();
        subscription.timeZone = timezone.toZoneId().getId();
        subscription.items = new ArrayList<>();
        subscription.user = getUser();

        for (Feed feed : request.feeds){
            SubscriptionItem item = new SubscriptionItem();
            item.feedId = feed.id;
            item.fullArticle = feed.fullArticle;
            item.markAsRead = feed.markAsRead;
            item.withImages = feed.includeImages;
            item.subscription = subscription;
            subscription.items.add(item);
        }

        JPA.withTransaction(() -> subscriptionDao.createSubscription(subscription));
        return F.Promise.pure(ok());
    }

    public Result getSubscription(String id) throws Throwable {
        return JPA.withTransaction(() -> {
            Subscription s = subscriptionDao.getSubscription(id);
            return ok(Json.toJson(map(s)));
        });
    }

    private SubscriptionModel map(Subscription subscription){
        SubscriptionModel response = new SubscriptionModel();
        response.time = LocalTime.parse(subscription.time).format(dateTimeFormatter());
        response.timezone = subscription.timeZone;
        response.feeds = new ArrayList<>();
        for (SubscriptionItem item : subscription.items){
            Feed itemResponse = new Feed();
            itemResponse.id = item.feedId;
            itemResponse.fullArticle = item.fullArticle;
            itemResponse.markAsRead = item.markAsRead;
            itemResponse.includeImages = item.withImages;
            response.feeds.add(itemResponse);
        }
        return response;
    }

    public Result saveSubscription(){
        return ok();
    }
}
