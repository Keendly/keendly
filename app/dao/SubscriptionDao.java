package dao;

import entities.Subscription;
import entities.SubscriptionItem;
import entities.User;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.util.List;

public class SubscriptionDao {

    public void createSubscription(Subscription subscription){
        JPA.em().persist(subscription);
    }

    public List<SubscriptionItem> getSubscriptionItems(User user){
        Query query = JPA.em().createQuery("select si from SubscriptionItem si where si.subscription.user = :user")
                .setParameter("user", user);
        return query.getResultList();
    }

    public Subscription getSubscription(String id){
        return JPA.em().find(Subscription.class, Long.parseLong(id));
    }
}
