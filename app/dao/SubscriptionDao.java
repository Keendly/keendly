package dao;

import models.Subscription;
import play.db.jpa.JPA;

public class SubscriptionDao {

    public void createSubscription(Subscription subscription){
        JPA.em().persist(subscription);
    }
}
