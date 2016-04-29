package com.keendly.dao;

import com.keendly.entities.DeliveryEntity;
import com.keendly.entities.DeliveryItemEntity;
import com.keendly.entities.UserEntity;
import play.db.jpa.JPA;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class DeliveryDao {

    public List<DeliveryEntity> getDeliveries(UserEntity user, int page, int pageSize){
        Query query = JPA.em().createQuery("select d from DeliveryEntity d where d.user = :user order by id desc")
                .setMaxResults(pageSize)
                .setFirstResult(pageSize * (page - 1))
                .setParameter("user", user);
        return query.getResultList();
    }

    public DeliveryEntity getDelivery(Long id){
        return JPA.em().find(DeliveryEntity.class, id);
    }

    public DeliveryEntity updateDelivery(DeliveryEntity deliveryEntity){
        return JPA.em().merge(deliveryEntity);
    }

    public void createDelivery(DeliveryEntity deliveryEntity){
        JPA.em().persist(deliveryEntity);
    }

    public DeliveryItemEntity getLastDeliveryItem(String feedId){
        Query query = JPA.em().createQuery("select di from DeliveryItemEntity di where di.feedId = :feedId and di.delivery.date is not null order by di.delivery.date desc")
                .setParameter("feedId", feedId)
                .setMaxResults(1);

        try {
            return (DeliveryItemEntity) query.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}
