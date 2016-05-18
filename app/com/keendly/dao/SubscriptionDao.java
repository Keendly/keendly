package com.keendly.dao;

import com.keendly.entities.SubscriptionEntity;
import com.keendly.entities.SubscriptionItemEntity;
import com.keendly.entities.UserEntity;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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

    public List<SubscriptionEntity> getDailySubscriptionsToDeliver(){
        // TODO should probably take into account ones that DO have deliveries but were not actually delivered
        Query query = JPA.em()
                .createNativeQuery("select s.id from subscription s " +
                        "where s.active = TRUE and s.frequency = 'DAILY' and not exists (" +
                        "   select id from delivery d where d.subscription_id = s.id " +
                        "       and d.created at time zone :system_timezone > case " +
                        "               when cast(now() at time zone s.timezone as time) > cast(s.time as time) " + // if today the scheduled hour has passed
                        "               then to_timestamp(to_char(now() at time zone s.timezone,'YYYY-MM-DD ')||s.time, 'YYYY-MM-DD HH24:MI') " + // then last scheduled delivery was today
                        "               else to_timestamp(to_char((now() at time zone s.timezone) - interval '1 day', 'YYYY-MM-DD ')||s.time, 'YYYY-MM-DD HH24:MI') " + // otherwise yesterday
                        "       end)").setParameter("system_timezone", TimeZone.getDefault().getID());
        List r = query.getResultList();
        List<Long> ids = new ArrayList<>();
        for (Object o : r) {
            ids.add(((BigInteger) o).longValue());
        }
        if (!ids.isEmpty()) {
            List res = JPA.em().createQuery("select s from SubscriptionEntity s where s.id in (:ids)")
                    .setParameter("ids", ids).getResultList();
            return res;
        } else {
            return Collections.emptyList();
        }

    }
}
