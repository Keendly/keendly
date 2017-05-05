package com.keendly.entities;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "KeendlyUser", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public Provider provider;

    @Column(name = "provider_id", nullable = false)
    public String providerId;

    @Column
    public String email;

    @Column(name = "delivery_email")
    public String deliveryEmail;

    @Column(length = 1000, name = "refresh_token")
    public String refreshToken;

    @Column(length = 1000, name = "access_token")
    public String accessToken;

    @Column(name = "last_login")
    public Date lastLogin;

    @Column(name = "delivery_sender")
    public String deliverySender;

    @Column(name = "notify_no_articles")
    public Boolean notifyNoArticles;
}
