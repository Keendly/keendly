package com.keendly.dao;

import com.keendly.entities.ClientEntity;
import play.db.jpa.JPA;

import javax.persistence.Query;

public class ClientDao {

    public ClientEntity findByClientId(String clientId){
        Query query = JPA.em().createQuery("select c from ClientEntity c where c.clientId = :clientId")
                .setParameter("clientId", clientId);

        return (ClientEntity) query.getSingleResult();
    }
}
