package models;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public Date date;

    @Column(nullable = false)
    public Boolean manual;

    @Column
    public String info;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public User user;

    @OneToMany(mappedBy = "delivery")
    public List<DeliveryItem> items;

    @ManyToOne(optional = true)
    @JoinColumn(name = "subscription_id", referencedColumnName = "id")
    public Subscription subscription;
}
