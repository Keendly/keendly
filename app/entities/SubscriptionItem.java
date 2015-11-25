package entities;

import javax.persistence.*;

@Entity
public class SubscriptionItem {

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_id", referencedColumnName = "id")
    public Subscription subscription;
}
