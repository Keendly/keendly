package com.keendly.schema.utils;

import com.keendly.adaptors.model.FeedEntry;
import com.keendly.model.Delivery;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;
import com.keendly.schema.DeliveryProtos;
import com.keendly.schema.DeliveryProtos.DeliveryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mapper {

    public static DeliveryRequest mapToDeliveryRequest(Delivery delivery, Map<String, List<FeedEntry>> unread,
                                                       long entityId, String deliveryEmail, long userId){

        DeliveryProtos.DeliveryRequest.Builder builder = DeliveryProtos.DeliveryRequest.newBuilder()
                .setId(entityId)
                .setEmail(deliveryEmail)
                .setUserId(userId)
                .setTimestamp(System.currentTimeMillis());
        for (Map.Entry<String, List<FeedEntry>> unreadFeed : unread.entrySet()){
            DeliveryItem deliveryItem
                    = delivery.items.stream().filter(item -> item.feedId.equals(unreadFeed.getKey())).findFirst().get();

            DeliveryProtos.DeliveryRequest.Item.Builder itemBuilder
                    = DeliveryProtos.DeliveryRequest.Item.newBuilder()
                    .setFeedId(unreadFeed.getKey())
                    .setTitle(deliveryItem.title)
                    .setWithImages(deliveryItem.includeImages)
                    .setMarkAsRead(deliveryItem.markAsRead)
                    .setFullArticle(deliveryItem.fullArticle);

            for (FeedEntry article : unreadFeed.getValue()){
                DeliveryProtos.DeliveryRequest.Item.Article article1
                        = DeliveryProtos.DeliveryRequest.Item.Article.newBuilder()
                        .setUrl(article.getUrl())
                        .setTitle(article.getTitle())
                        .setAuthor(article.getAuthor())
                        .setTimestamp(article.getPublished().getTime())
                        .setContent(article.getContent()).build();

                itemBuilder.addArticles(article1);
            }

            if (itemBuilder.getArticlesCount() > 0){
                builder.addItems(itemBuilder.build());
            }
        }

        return builder.build();
    }

    public static com.keendly.model.DeliveryRequest toDeliveryRequest(Delivery delivery, Map<String, List<FeedEntry>> unread,
                                                                      long entityId, String deliveryEmail, long userId){
        com.keendly.model.DeliveryRequest request = new com.keendly.model.DeliveryRequest();
        request.email = deliveryEmail;
        request.id = entityId;
        request.userId = userId;
        request.timestamp = System.currentTimeMillis();

        request.items = new ArrayList<>();
        for (Map.Entry<String, List<FeedEntry>> unreadFeed : unread.entrySet()){
            DeliveryItem deliveryItem
                    = delivery.items.stream().filter(item -> item.feedId.equals(unreadFeed.getKey())).findFirst().get();

            deliveryItem.articles = new ArrayList<>();
            for (FeedEntry article : unreadFeed.getValue()){
                DeliveryArticle deliveryArticle = new DeliveryArticle();
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
