package controllers;

import adaptors.Adaptor;
import adaptors.model.SubscribedFeed;
import auth.Tokens;
import dao.DeliveryDao;
import dao.SubscriptionDao;
import entities.DeliveryItemEntity;
import entities.SubscriptionItemEntity;
import model.Delivery;
import model.Feed;
import model.Subscription;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static controllers.RequestUtils.getAdaptor;
import static controllers.RequestUtils.getTokens;

@With(SecuredAction.class)
public class FeedController extends AbstractController {

    private SubscriptionDao subscriptionDao = new SubscriptionDao();
    private DeliveryDao deliveryDao = new DeliveryDao();
    private FeedMapper feedMapper = new FeedMapper();

    public Promise<Result> getFeeds(){
        return adaptor().getSubscribedFeeds(tokens()).map(subscribedFeeds ->
                JPA.withTransaction(() -> {
                    List<Feed> feeds = new ArrayList<>();

                    List<SubscriptionItemEntity> subscriptionItemEntities =
                            subscriptionDao.getSubscriptionItems(getUserEntity());

                    for(SubscribedFeed subscribedFeed : subscribedFeeds){
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
                                subscription.time = LocalTime.parse(feedSubItemEntity.subscription.time)
                                        .format(dateTimeFormatter());
                                feed.subscriptions.add(subscription);
                            }
                        }

                        // TODO add last delivery
                        DeliveryItemEntity lastDeliveryItem = deliveryDao.getLastDeliveryItem(subscribedFeed.getFeedId());
                        if (lastDeliveryItem != null){
                            Delivery delivery = new Delivery();
                            delivery.id = lastDeliveryItem.delivery.id;
                            delivery.deliveryDate = lastDeliveryItem.delivery.date;
                            feed.lastDelivery = delivery;
                        }
                        feeds.add(feed);
                    }

                    return ok(Json.toJson(feeds));
                })
        );
    }

    private Adaptor adaptor(){
        return getAdaptor(ctx().request());
    }

    private Tokens tokens(){
        return getTokens(ctx().request());
    }

    static class FeedMapper {

        Feed toModel(SubscribedFeed external){
            Feed feed = new Feed();
            feed.feedId = external.getFeedId();
            feed.title = external.getTitle();

            return feed;
        }

    }

}
