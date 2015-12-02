package controllers;

import adaptors.model.ExternalSubscription;
import controllers.model.FeedDelivery;
import controllers.model.FeedSubscription;
import dao.DeliveryDao;
import dao.SubscriptionDao;
import dao.UserDao;
import entities.Delivery;
import entities.DeliveryItem;
import entities.SubscriptionItem;
import entities.User;
import org.apache.commons.lang3.StringUtils;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.With;
import sun.util.calendar.ZoneInfo;
import views.html.history;
import views.html.home;
import views.html.settings;

import java.time.LocalTime;
import java.time.zone.ZoneRulesProvider;
import java.util.*;
import java.util.stream.Collectors;


@With(SecuredAction.class)
public class SecuredController extends AbstractController {

    private static Map<String, Integer> zones = new TreeMap<>();
    static {
        for (String zone : ZoneRulesProvider.getAvailableZoneIds()){
            zones.put(zone, ZoneInfo.getTimeZone(zone).getOffset(System.currentTimeMillis())/1000/60/60);
        }
    }

    private SubscriptionDao subscriptionDao = new SubscriptionDao();
    private DeliveryDao deliveryDao = new DeliveryDao();
    private UserDao userDao = new UserDao();

    public Promise<Result> home(){
        return findAdaptor().getSubscriptions(findTokens()).map(subscriptions ->
            JPA.withTransaction(() -> {
                List<SubscriptionItem> items = subscriptionDao.getSubscriptionItems(getUser());
                List<FeedSubscription> feedSubscriptions = new ArrayList<>();

                for(ExternalSubscription externalSubscription : subscriptions){
                    List<SubscriptionItem> feedItems = items.stream()
                            .filter(s -> s.feedId.equals(externalSubscription.getFeedId()))
                            .collect(Collectors.toList());
                    FeedSubscription feedSubscription = map(externalSubscription, feedItems);
                    feedSubscriptions.add(feedSubscription);
                }
                return ok(home.render(feedSubscriptions, zones, dateTimeFormatter(), session()));
            })
        );
    }

    private FeedSubscription map(ExternalSubscription externalSubscription, List<SubscriptionItem> feedItems){
        FeedSubscription feedSubscription = new FeedSubscription();
        feedSubscription.providerId = externalSubscription.getFeedId();
        feedSubscription.title = externalSubscription.getTitle();
        Map<LocalTime, Long> scheduled = new HashMap<>();
        for (SubscriptionItem item : feedItems){
            scheduled.put(LocalTime.parse(item.subscription.time), item.subscription.id);
        }
        feedSubscription.scheduled = scheduled;

        return feedSubscription;
    }

    public Result history(){
        int page = parsePage(getQueryParam("page"));
        List<FeedDelivery> deliveries = new ArrayList<>();
        JPA.withTransaction(() ->  {
            List<Delivery> entities = deliveryDao.getDeliveries(getUser(), page);
            deliveries.addAll(map(entities));
        });
        return ok(history.render(deliveries, page, lang()));
    }

    private List<FeedDelivery> map(List<Delivery> deliveries){
        List<FeedDelivery> ret = new ArrayList<>();
        for (Delivery delivery : deliveries){
            FeedDelivery feedDelivery = new FeedDelivery();
            feedDelivery.date = delivery.date;
            List<String> feeds = new ArrayList<>();
            feedDelivery.feeds = feeds;
            for (DeliveryItem deliveryItem : delivery.items){
                feeds.add(deliveryItem.title);
            }
            ret.add(feedDelivery);
        }
        return ret;
    }

    private int parsePage(String page){
        if (StringUtils.isEmpty(page)){
            return 1;
        }
        try {
            return Integer.parseInt(page);
        } catch (Exception e){
            return 1;
        }
    }

    public Result getSettings(){
        final StringBuffer email = new StringBuffer();
        JPA.withTransaction(() -> {
            User user = userDao.findById(getUser().id);
            email.append(user.deliveryEmail);
        });
        return ok(settings.render(email.toString(), flash()));
    }

    public Result saveSettings(){
        Map<String, String[]> form = request().body().asFormUrlEncoded();
        JPA.withTransaction(() -> {
            User user = userDao.findById(getUser().id);
            user.deliveryEmail = form.get("email")[0];
        });
        flash(Constants.FLASH_INFO, Messages.get("settings.saved"));
        return redirect(routes.SecuredController.getSettings());
    }
}
