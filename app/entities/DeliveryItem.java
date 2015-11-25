package entities;

import javax.persistence.*;
import java.util.List;

@Entity
public class DeliveryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(name = "feed_id", nullable = false)
    public String feedId;

    @Column(name = "with_images", nullable = false)
    public Boolean withImages;

    @Column(name = "full_article", nullable = false)
    public Boolean fullArticle;

    @Column(name = "mark_as_read", nullable = false)
    public Boolean markAsRead;

    @Column(nullable = false)
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delivery_id", referencedColumnName = "id")
    public Delivery delivery;

    @OneToMany(mappedBy = "deliveryItem")
    public List<DeliveryArticle> articles;
}
