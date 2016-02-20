package com.keendly.entities;

import javax.persistence.*;

@Entity
@Table(name = "DeliveryArticle")
public class DeliveryArticleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    public String url;

    @Column(nullable = false)
    public String title;

    @ManyToOne
    @JoinColumn(name = "delivery_item_id", referencedColumnName = "id")
    public DeliveryItemEntity deliveryItem;
}
