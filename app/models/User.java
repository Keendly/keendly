package models;

import javax.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public String id;

    @Column
    @Enumerated(EnumType.STRING)
    public Provider provider;

    @Column(name = "provider_id")
    public String providerId;
}
