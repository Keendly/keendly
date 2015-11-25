package entities;

import javax.persistence.*;

@Entity
public class DeliveryArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public String url;

    @Column(nullable = false)
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delivery_item_id", referencedColumnName = "id")
    public DeliveryItem deliveryItem;
}
