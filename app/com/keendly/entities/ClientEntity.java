package com.keendly.entities;

import javax.persistence.*;

@Entity
@Table(name = "Client", indexes = {
        @Index(name = "client_id_idx", columnList = "client_id", unique = true)
})
public class ClientEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "client_id", nullable = false)
    public String clientId;

    @Column(name = "client_secret", nullable = false)
    public String clientSecret;

}
