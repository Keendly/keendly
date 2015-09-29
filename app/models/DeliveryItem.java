package models;

import javax.persistence.*;
import java.util.List;

@Entity
public class DeliveryItem {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public String id;

    @Column(name = "feed_id")
    public String feedId;

    @Column(name = "with_images")
    public Boolean withImages;

    @Column(name = "full_article")
    public Boolean fullArticle;

    @Column(name = "mark_as_read")
    public Boolean markAsRead;

    @Column(name = "feed_title")
    public String feedTitle;

    @ManyToOne(optional = false)
    @JoinColumn(name="delivery_id", referencedColumnName="id")
    public Delivery delivery;

    @OneToMany(mappedBy = "deliveryItem")
    public List<DeliveryArticle> articles;
}
