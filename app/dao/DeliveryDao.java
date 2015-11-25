package dao;

import entities.Delivery;
import entities.User;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.util.List;

public class DeliveryDao {

    public static int PAGE_SIZE = 10;

    public List<Delivery> getDeliveries(User user, int page){
        Query query = JPA.em().createQuery("select d from Delivery d where d.user = :user order by date desc")
                .setMaxResults(PAGE_SIZE)
                .setFirstResult(PAGE_SIZE * (page - 1) + 1)
                .setParameter("user", user);
        return query.getResultList();
    }
}
