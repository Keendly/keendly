package com.keendly.entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "Subscription")
public class SubscriptionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public String time;

    @Column(nullable = false)
    public String timeZone;

    @Column
    @Enumerated(EnumType.STRING)
    public SubscriptionFrequency frequency;

    @Column(nullable = false)
    public Boolean active;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.PERSIST)
    public List<SubscriptionItemEntity> items;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public UserEntity user;
}
