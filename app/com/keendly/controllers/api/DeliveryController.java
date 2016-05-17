package com.keendly.controllers.api;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.controllers.api.error.Error;
import com.keendly.dao.DeliveryDao;
import com.keendly.entities.*;
import com.keendly.mappers.MappingMode;
import com.keendly.model.Delivery;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;
import com.keendly.model.Subscription;
import com.keendly.schema.DeliveryProtos;
import com.keendly.schema.utils.Mapper;
import com.keendly.utils.ConfigUtils;
import org.apache.commons.lang3.RandomStringUtils;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@With(com.keendly.controllers.api.SecuredAction.class)
public class DeliveryController extends com.keendly.controllers.api.AbstractController<Delivery> {

    private static final play.Logger.ALogger LOG = play.Logger.of(DeliveryController.class);

    private static String S3_BUCKET = ConfigUtils.parameter("s3.bucket_name");
    private static String S3_PATH = ConfigUtils.parameter("s3.delivery_path");

    private static int MAX_FEEDS_IN_DELIVERY = 20;

    private AmazonS3Client amazonS3Client = new AmazonS3Client();

    private DeliveryDao deliveryDao = new DeliveryDao();
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    public Promise<Result> createDelivery() {
        // HACK WARNING
        StringBuilder deliveryEmail = new StringBuilder();
        StringBuilder userId = new StringBuilder();
        JPA.withTransaction(() -> {
            UserEntity userEntity = new UserController().lookupUser("self");
            if (userEntity.deliveryEmail != null){
                deliveryEmail.append(userEntity.deliveryEmail);
                userId.append(userEntity.id);
            }
        });
        if (deliveryEmail.toString().isEmpty()){
            return Promise.pure(badRequest(toJson(Error.DELIVERY_EMAIL_NOT_CONFIGURED)));
        }

        Delivery delivery = fromRequest();

        if (delivery.items.size() > MAX_FEEDS_IN_DELIVERY){
            return Promise.pure(badRequest(toJson(Error.TOO_MANY_ITEMS, MAX_FEEDS_IN_DELIVERY)));
        }

        DeliveryEntity deliveryEntity = deliveryMapper.toEntity(delivery);
        JPA.withTransaction(() ->  deliveryDao.createDelivery(deliveryEntity));

        List<String> feedIds = delivery.items.stream().map(item -> item.feedId).collect(Collectors.toList());
        return getAdaptor().getUnread(feedIds).map(unread -> {

            DeliveryProtos.DeliveryRequest deliveryRequest
                    = Mapper.mapToDeliveryRequest(delivery, unread, deliveryEntity.id, deliveryEmail.toString(),
                    Long.parseLong(userId.toString()));

            try {
                String uid = generateDirName();
                storeInS3(deliveryRequest, uid);
                deliveryEntity.s3Dir = uid;
                JPA.withTransaction(() -> deliveryDao.updateDelivery(deliveryEntity));
                LOG.debug("Delivery request for id {} stored in s3 dir {}", deliveryEntity.id, uid);
            } catch (Exception e){
                LOG.error("Error storing delivery to S3", e);
                return internalServerError();
            }

           return ok(Json.toJson(deliveryMapper.toModel(deliveryEntity, MappingMode.SIMPLE)));
        });
    }

    private JsonNode toJson(Error error, Object... msgParams){
        Map<String, String> map = new HashMap<>();
        map.put("code", error.name());
        map.put("description", String.format(error.getMessage(), msgParams));
        return Json.toJson(map);
    }

    private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssS");
    private String generateDirName(){
        String d = format.format(new Date());
        return d + "_" + RandomStringUtils.randomAlphanumeric(5);
    }

    private void storeInS3(DeliveryProtos.DeliveryRequest deliveryRequest, String uid) throws IOException {
        File f = new File("/tmp/" + uid);
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        deliveryRequest.writeTo(fos);
        amazonS3Client.putObject(new PutObjectRequest(S3_BUCKET, S3_PATH + "/" + uid + "/delivery.req", f));
    }

    public Promise<Result> updateDelivery(Long id) {
        // what the hack
        Delivery delivery = fromRequest();
        boolean[] markAsRead = {false};
        long[] created = new long[1];
        List<String> feedsToMarkAsRead = new ArrayList<>();
        boolean[] forbidden = {false};
        try {
            JPA.withTransaction(() -> {
                UserEntity user = getUserEntity();
                DeliveryEntity currentEntity = deliveryDao.getDelivery(id);
                boolean wasDeliveredBefore = currentEntity.date != null;
                if (user.id == currentEntity.user.id){
                    deliveryMapper.toEntity(delivery, currentEntity);
                    DeliveryEntity updated = deliveryDao.updateDelivery(currentEntity);
                    boolean isDelivered = updated.date != null;
                    if (!wasDeliveredBefore && isDelivered){
                        markAsRead[0] = true;
                        created[0] = updated.created.getTime();
                        for (DeliveryItemEntity deliveryItem : updated.items){
                            if (deliveryItem.markAsRead){
                                feedsToMarkAsRead.add(deliveryItem.feedId);
                            }
                        }
                    }

                } else {
                    LOG.error("Authenticated user id ({}) does not match delivery user id ({})",
                            getUserEntity().id, currentEntity.user.id);
                    forbidden[0] = true;

                }
            });
        } catch (Throwable throwable) {
            LOG.error("Error updating delivery " + id, throwable);
            return Promise.pure(internalServerError());
        }
        if (forbidden[0]){
            return Promise.pure(forbidden());
        }
        if (markAsRead[0]){
            return getAdaptor().markAsRead(feedsToMarkAsRead, created[0]).map(success -> {
                if (success){
                    return ok();
                } else {
                    return internalServerError();
                }
            });
        } else {
            return Promise.pure(ok());
        }
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
            entity.user = getDummyUserEntity();
            entity.manual = delivery.manual;
            toEntity(delivery, entity);
            return entity;
        }

        void toEntity(Delivery delivery, DeliveryEntity entity){
            if (delivery.deliveryDate != null){
                entity.date = delivery.deliveryDate;
            }
            if (delivery.error != null){
                entity.errorDescription = delivery.error;
            }
            if (delivery.subscription != null){
                SubscriptionEntity se = new SubscriptionEntity();
                se.id = delivery.subscription.id;
                entity.subscription = se;
            }
            for (DeliveryItem item : delivery.items){
                DeliveryItemEntity itemEntity;
                if (item.id != null){
                    Optional<DeliveryItemEntity> s = entity.items.stream().filter(it -> it.id == item.id).findFirst();
                    if (!s.isPresent()){
                        LOG.warn("Wrong number of found items with id: {}, skipping", item.id);
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
            delivery.error = entity.errorDescription;
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
            if (entity.subscription != null){
                Subscription s = new Subscription();
                s.id = entity.subscription.id;
                delivery.subscription = s;
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
