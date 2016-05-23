package com.keendly.entities;

import javax.persistence.*;

@Entity
@Table(name = "SubscriptionItem")
public class SubscriptionItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(name = "feed_id", nullable = false)
    public String feedId;

    @Column(nullable = false)
    public String title;

    @Column(name = "with_images", nullable = false)
    public Boolean withImages;

    @Column(name = "full_article", nullable = false)
    public Boolean fullArticle;

    @Column(name = "mark_as_read", nullable = false)
    public Boolean markAsRead;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_id", referencedColumnName = "id")
    public SubscriptionEntity subscription;
}
