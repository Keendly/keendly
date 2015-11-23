package dao;

import models.Provider;
import models.User;
import play.db.jpa.JPA;

import javax.persistence.NoResultException;
import javax.persistence.Query;

public class UserDao {

    public User findByProviderId(String id, Provider provider){
        Query query = JPA.em().createQuery("select u from User u where u.provider = :provider and u.providerId = :providerId")
                .setParameter("provider", provider)
                .setParameter("providerId", id);

        try {
            return (User) query.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }

    public User createUser(String id, Provider provider, String email){
        User user = new User();
        user.providerId = id;
        user.provider = provider;
        user.email = email;
        JPA.em().persist(user);
        return user;
    }
}
