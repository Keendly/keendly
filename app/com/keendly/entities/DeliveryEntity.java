package com.keendly.entities;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Delivery")
public class DeliveryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column
    public Date date; // null until delivered actually

    @Column(nullable = false)
    public Boolean manual;

    @Column
    public String info;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public UserEntity user;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.PERSIST)
    public List<DeliveryItemEntity> items;

    @ManyToOne(optional = true)
    @JoinColumn(name = "subscription_id", referencedColumnName = "id")
    public SubscriptionEntity subscription;
}
