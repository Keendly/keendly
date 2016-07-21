package com.keendly.controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.auth.Token;
import com.keendly.dao.DeliveryDao;
import com.keendly.dao.SubscriptionDao;
import com.keendly.dao.UserDao;
import com.keendly.entities.DeliveryItemEntity;
import com.keendly.entities.SubscriptionItemEntity;
import com.keendly.entities.UserEntity;
import com.keendly.model.Delivery;
import com.keendly.model.Feed;
import com.keendly.model.Subscription;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@With(SecuredAction.class)
public class FeedController extends AbstractController<Feed> {

    private static Logger LOG = Logger.getLogger(FeedController.class.getCanonicalName());

    private SubscriptionDao subscriptionDao = new SubscriptionDao();
    private DeliveryDao deliveryDao = new DeliveryDao();
    private UserDao userDao = new UserDao();
    private FeedMapper feedMapper = new FeedMapper();

    public Promise<? extends Result> getFeeds(){
        Adaptor adaptor = getAdaptor();

        return adaptor.getFeeds().map(subscribedFeeds ->
                JPA.withTransaction(() -> {
                    List<Feed> feeds = new ArrayList<>();

                    List<SubscriptionItemEntity> subscriptionItemEntities =
                            subscriptionDao.getSubscriptionItems(getUserEntity());

                    for(ExternalFeed subscribedFeed : subscribedFeeds){
                        // find items for that feed
                        List<SubscriptionItemEntity> feedSubItemEntities = subscriptionItemEntities.stream()
                                .filter(s -> s.feedId.equals(subscribedFeed.getFeedId()))
                                .collect(Collectors.toList());

                        Feed feed = feedMapper.toModel(subscribedFeed);

                        if (!feedSubItemEntities.isEmpty()){
                            feed.subscriptions = new ArrayList<>();
                            for (SubscriptionItemEntity feedSubItemEntity : feedSubItemEntities){
                                // TODO move to proper mapper, duplication with SubscriptionController:map
                                Subscription subscription = new Subscription();
                                subscription.id = feedSubItemEntity.subscription.id;
                                subscription.time = feedSubItemEntity.subscription.time;
                                subscription.timezone = feedSubItemEntity.subscription.timeZone;
                                feed.subscriptions.add(subscription);
                            }
                        }

                        DeliveryItemEntity lastDeliveryItem = deliveryDao.getLastDeliveryItem(subscribedFeed.getFeedId());
                        if (lastDeliveryItem != null){
                            Delivery delivery = new Delivery();
                            delivery.id = lastDeliveryItem.delivery.id;
                            delivery.deliveryDate = lastDeliveryItem.delivery.date;
                            feed.lastDelivery = delivery;
                        }
                        feeds.add(feed);
                    }
                    refreshTokenIfNeeded(adaptor.getToken());
                    return ok(Json.toJson(feeds));
                })
        ).recover(error -> {
            if (error instanceof ApiException){
                ApiException exception = (ApiException) error;
                return status(exception.getStatus(), exception.getResponse());
            }
            throw error;
        });
    }


    protected void refreshTokenIfNeeded(Token externalToken){
        if (externalToken.gotRefreshed()){
            UserEntity entity = getUserEntity();
            entity.accessToken = externalToken.getAccessToken();
            JPA.em().merge(entity);
        }
    }

    public Promise<Result> getUnread(){
        JsonNode node = request().body().asJson();
        List<String> feedIds = new ArrayList<>();
        for (JsonNode id : node){
            feedIds.add(id.asText());
        }
        return getAdaptor().getUnread(feedIds).map(response -> {
            return ok(Json.toJson(response));
        });
    }

    public Promise<Result> getUnreadCount(){
        JsonNode node = request().body().asJson();
        List<String> feedIds = new ArrayList<>();
        for (JsonNode id : node){
            feedIds.add(id.asText());
        }
        return getAdaptor().getUnreadCount(feedIds).map(response -> {
            return ok(Json.toJson(response));
        });
    }

    static class FeedMapper {

        Feed toModel(ExternalFeed external){
            Feed feed = new Feed();
            feed.feedId = external.getFeedId();
            feed.title = external.getTitle();

            return feed;
        }
    }

}
