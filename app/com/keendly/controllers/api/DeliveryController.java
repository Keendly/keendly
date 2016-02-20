package com.keendly.controllers.api;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.keendly.adaptors.model.Entry;
import com.keendly.dao.DeliveryDao;
import com.keendly.entities.DeliveryArticleEntity;
import com.keendly.entities.DeliveryEntity;
import com.keendly.entities.DeliveryItemEntity;
import com.keendly.mappers.MappingMode;
import com.keendly.model.Delivery;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;
import com.keendly.schema.DeliveryProtos;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@With(com.keendly.controllers.api.SecuredAction.class)
public class DeliveryController extends com.keendly.controllers.api.AbstractController<Delivery> {

    private static Logger LOG = Logger.getLogger(DeliveryController.class.getCanonicalName());

    private DeliveryDao deliveryDao = new DeliveryDao();
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    private static AmazonS3Client amazonS3Client = new AmazonS3Client();

    public Promise<Result> createDelivery() {
        // validate if delivery email is configured
        Delivery delivery = fromRequest();
        DeliveryEntity deliveryEntity = deliveryMapper.toEntity(delivery);
        JPA.withTransaction(() ->  deliveryDao.createDelivery(deliveryEntity));

        List<String> feedIds = delivery.items.stream().map(item -> item.feedId).collect(Collectors.toList());
        return getAdaptor().getUnread(feedIds).map(unread -> {

            DeliveryProtos.DeliveryRequest.Builder builder = DeliveryProtos.DeliveryRequest.newBuilder()
                    .setId(deliveryEntity.id)
                   // .setEmail(getUserEntity().deliveryEmail)
                    .setTimestamp(System.currentTimeMillis());
            for (Map.Entry<String, List<Entry>> unreadFeed : unread.entrySet()){
                DeliveryItem deliveryItem
                        = delivery.items.stream().filter(item -> item.feedId.equals(unreadFeed.getKey())).findFirst().get();

                DeliveryProtos.DeliveryRequest.Item.Builder itemBuilder
                        = DeliveryProtos.DeliveryRequest.Item.newBuilder()
                        .setFeedId(unreadFeed.getKey())
                        .setTitle(deliveryItem.title)
                        .setWithImages(deliveryItem.includeImages)
                        .setMarkAsRead(deliveryItem.markAsRead)
                        .setFullArticle(deliveryItem.fullArticle);

                for (Entry article : unreadFeed.getValue()){
                    DeliveryProtos.DeliveryRequest.Item.Article article1
                            = DeliveryProtos.DeliveryRequest.Item.Article.newBuilder()
                            .setUrl(article.getUrl())
                            .setTitle(article.getTitle())
                            .setAuthor(article.getAuthor())
                            .setTimestamp(article.getPublished().getTime())
                            .setContent(article.getContent()).build();

                    itemBuilder.addArticles(article1);
                }

                builder.addItems(itemBuilder.build());
            }

            File f = new File("/tmp/elo");
            DeliveryProtos.DeliveryRequest deliveryRequest = builder.build();
            FileOutputStream fos = new FileOutputStream(f);
            deliveryRequest.writeTo(fos);

            amazonS3Client.putObject(new PutObjectRequest("keendly", "delivery.request", f));
           return status(202);
        });
    }

    public Promise<Result> updateDelivery(Long id) {
        Delivery delivery = fromRequest();
        JPA.withTransaction(() -> {
            DeliveryEntity currentEntity = deliveryDao.getDelivery(id);
            deliveryMapper.toEntity(delivery, currentEntity);
            deliveryDao.updateDelivery(currentEntity);
        });

        return Promise.pure(ok());
    }

    public Promise<Result> getDelivery(Long id) {
        List<Delivery> deliveries = new ArrayList<>();
        JPA.withTransaction(() -> {
            DeliveryEntity entity = deliveryDao.getDelivery(id);
            if (entity != null) {
                Delivery delivery = deliveryMapper.toModel(entity, MappingMode.FULL);
                deliveries.add(delivery);
            }
        });
        if (deliveries.isEmpty()){
            return Promise.pure(notFound());
        } else {
            return Promise.pure(ok(Json.toJson(deliveries.get(0))));
        }
    }

    public Promise<Result> getDeliveries(int page, int pageSize) {
        List<Delivery> deliveries = new ArrayList<>();
        JPA.withTransaction(() -> {
            List<DeliveryEntity> entities = deliveryDao.getDeliveries(getUserEntity(), page, pageSize);
            deliveries.addAll(deliveryMapper.toModel(entities, MappingMode.SIMPLE));
        });

        return Promise.pure(ok(Json.toJson(deliveries)));
    }

    class DeliveryMapper {

        DeliveryEntity toEntity(Delivery delivery){
            DeliveryEntity entity = new DeliveryEntity();
            entity.items = new ArrayList<>();
            entity.user = getUserEntity();
            entity.manual = true;
            toEntity(delivery, entity);
            return entity;
        }

        void toEntity(Delivery delivery, DeliveryEntity entity){
            if (delivery.deliveryDate != null){
                entity.date = delivery.deliveryDate;
            }
            for (DeliveryItem item : delivery.items){
                DeliveryItemEntity itemEntity;
                if (item.id != null){
                    Optional<DeliveryItemEntity> s = entity.items.stream().filter(it -> it.id == item.id).findFirst();
                    if (!s.isPresent()){
                        LOG.warning(String.format("Wrong number of found items with id: %d, skipping", item.id));
                        continue;
                    }
                    itemEntity = s.get();
                } else {
                    itemEntity = new DeliveryItemEntity();
                    itemEntity.delivery = entity;
                    entity.items.add(itemEntity);
                }

                if (item.feedId != null) {
                    itemEntity.feedId = item.feedId;
                }
                if (item.title != null) {
                    itemEntity.title = item.title;
                }
                if (item.fullArticle != null) {
                    itemEntity.fullArticle  = item.fullArticle;
                }
                if (item.markAsRead != null) {
                    itemEntity.markAsRead = item.markAsRead;
                }
                if (item.includeImages != null) {
                    itemEntity.withImages = item.includeImages;
                }
                if (item.articles != null) {
                    // replacing existing articles if provided, updating existing articles not supported
                    if (itemEntity.articles != null){
                        for (DeliveryArticleEntity articleEntity : itemEntity.articles){
                            articleEntity.deliveryItem = null;
                        }
                        itemEntity.articles.clear();
                    }
                    for (DeliveryArticle article : item.articles) {
                        DeliveryArticleEntity articleEntity = new DeliveryArticleEntity();
                        articleEntity.deliveryItem = itemEntity;
                        articleEntity.title = article.title;
                        articleEntity.url = article.url;
                        itemEntity.articles.add(articleEntity);
                    }
                }
            }
        }

        Delivery toModel(DeliveryEntity entity, MappingMode mode){
            Delivery delivery = new Delivery();
            delivery.id = entity.id;
            delivery.deliveryDate = entity.date;
            List<DeliveryItem> feeds = new ArrayList<>();
            delivery.items = feeds;
            for (DeliveryItemEntity deliveryItem : entity.items) {
                DeliveryItem feed = new DeliveryItem();
                feed.id = deliveryItem.id;
                feed.title = deliveryItem.title;
                feed.feedId = deliveryItem.feedId;

                if (mode == MappingMode.FULL){
                    feed.fullArticle = deliveryItem.fullArticle;
                    feed.includeImages = deliveryItem.withImages;
                    feed.markAsRead = deliveryItem.markAsRead;

                    List<DeliveryArticle> articles = new ArrayList<>();
                    feed.articles = articles;
                    for (DeliveryArticleEntity articleEntity : deliveryItem.articles){
                        DeliveryArticle article = new DeliveryArticle();
                        article.title = articleEntity.title;
                        article.url = articleEntity.url;
                        articles.add(article);
                    }
                }
                feeds.add(feed);
            }

            return delivery;
        }

        List<Delivery> toModel(List<DeliveryEntity> entities, MappingMode mode){
            List<Delivery> deliveries = new ArrayList<>();
            for (DeliveryEntity entity : entities) {
                deliveries.add(toModel(entity, mode));
            }
            return deliveries;
        }
    }
}
