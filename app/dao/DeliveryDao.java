package dao;

import entities.DeliveryEntity;
import entities.UserEntity;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.util.List;

public class DeliveryDao {

    public List<DeliveryEntity> getDeliveries(UserEntity user, int page, int pageSize){
        Query query = JPA.em().createQuery("select d from DeliveryEntity d where d.user = :user order by date desc")
                .setMaxResults(pageSize)
                .setFirstResult(pageSize * (page - 1))
                .setParameter("user", user);
        return query.getResultList();
    }

    public DeliveryEntity getDelivery(Long id){
        return JPA.em().find(DeliveryEntity.class, id);
    }

    public void updateDelivery(DeliveryEntity deliveryEntity){
        JPA.em().merge(deliveryEntity);
    }

    public void createDelivery(DeliveryEntity deliveryEntity){
        JPA.em().persist(deliveryEntity);
    }
}
