package models;

import javax.persistence.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Entity
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public LocalTime time;

    @Column(nullable = false)
    public ZoneId timeZone;

    @Column
    @Enumerated(EnumType.STRING)
    public SubscriptionFrequency frequency;

    @Column(nullable = false)
    public Boolean active;

    @OneToMany(mappedBy = "subscription")
    public List<SubscriptionItem> items;
}
