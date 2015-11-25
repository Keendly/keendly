package entities;

import javax.persistence.*;
import java.util.List;

@Entity
public class Subscription extends BaseEntity {

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
    public List<SubscriptionItem> items;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public User user;
}
