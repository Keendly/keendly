package models;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class Delivery {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public String id;

    @Column
    public Date date;

    @Column
    public Boolean manual;

    @Column
    @Enumerated(EnumType.STRING)
    public Provider provider;

    @Column
    public String info;

    @ManyToOne(optional = false)
    @JoinColumn(name="user_id", referencedColumnName="id")
    public User user;

    @OneToMany(mappedBy = "delivery")
    public List<DeliveryItem> items;
}
