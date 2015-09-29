package models;

import javax.persistence.*;

public class DeliveryArticle {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public String id;

    @Column
    public String url;

    @Column
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(name="delivery_item_id", referencedColumnName="id")
    public DeliveryItem deliveryItem;
}
