package dao;

import entities.Provider;
import entities.UserEntity;
import play.db.jpa.JPA;

import javax.persistence.NoResultException;
import javax.persistence.Query;

public class UserDao {

    public UserEntity findByProviderId(String id, Provider provider){
        Query query = JPA.em().createQuery("select u from UserEntity u where u.provider = :provider and u.providerId = :providerId")
                .setParameter("provider", provider)
                .setParameter("providerId", id);

        try {
            return (UserEntity) query.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }

    public UserEntity createUser(String id, Provider provider, String email){
        UserEntity user = new UserEntity();
        user.providerId = id;
        user.provider = provider;
        user.email = email;
        JPA.em().persist(user);
        return user;
    }

    public UserEntity updateUser(UserEntity user){
        return JPA.em().merge(user);
    }

    public UserEntity findById(long id){
        return JPA.em().find(UserEntity.class, id);
    }
}
