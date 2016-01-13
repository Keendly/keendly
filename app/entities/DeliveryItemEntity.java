package entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "DeliveryItem")
public class DeliveryItemEntity extends BaseEntity {

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
    public DeliveryEntity delivery;

    @OneToMany(mappedBy = "deliveryItem", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    public List<DeliveryArticleEntity> articles;
}
