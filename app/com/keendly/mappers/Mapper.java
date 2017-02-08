package com.keendly.mappers;

import com.keendly.adaptors.model.FeedEntry;
import com.keendly.entities.Provider;
import com.keendly.model.Delivery;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mapper {

    public static com.keendly.model.DeliveryRequest toDeliveryRequest(Delivery delivery, Map<String, List<FeedEntry>> unread,
                                                                      long entityId, String deliveryEmail, long userId,
                                                                      Provider provider){
        com.keendly.model.DeliveryRequest request = new com.keendly.model.DeliveryRequest();
        request.email = deliveryEmail;
        request.id = entityId;
        request.userId = userId;
        request.timestamp = System.currentTimeMillis();
        request.provider = provider;

        request.items = new ArrayList<>();
        for (Map.Entry<String, List<FeedEntry>> unreadFeed : unread.entrySet()){
            DeliveryItem deliveryItem
                    = delivery.items.stream().filter(item -> item.feedId.equals(unreadFeed.getKey())).findFirst().get();

            deliveryItem.articles = new ArrayList<>();
            for (FeedEntry article : unreadFeed.getValue()){
                DeliveryArticle deliveryArticle = new DeliveryArticle();
                deliveryArticle.id = article.getId();
                deliveryArticle.url = article.getUrl();
                deliveryArticle.title = article.getTitle();
                deliveryArticle.author = article.getAuthor();
                deliveryArticle.timestamp = article.getPublished().getTime();
                deliveryArticle.content = article.getContent();

                deliveryItem.articles.add(deliveryArticle);
            }

            if (!deliveryItem.articles.isEmpty()){
                request.items.add(deliveryItem);
            }
        }

        return request;
    }
}
