package dao;

import entities.SubscriptionEntity;
import entities.SubscriptionItemEntity;
import entities.UserEntity;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.util.List;

public class SubscriptionDao {

    public void createSubscription(SubscriptionEntity subscription){
        JPA.em().persist(subscription);
    }

    public SubscriptionEntity getSubscription(String id){
        return JPA.em().find(SubscriptionEntity.class, Long.parseLong(id));
    }

    public List<SubscriptionItemEntity> getSubscriptionItems(UserEntity user){
        Query query = JPA.em()
                .createQuery("select si from SubscriptionItemEntity si where si.subscription.user = :user")
                .setParameter("user", user);
        return query.getResultList();
    }
}
