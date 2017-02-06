package com.keendly.controllers.api;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.*;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.model.FeedEntry;
import com.keendly.controllers.api.error.Error;
import com.keendly.dao.DeliveryDao;
import com.keendly.entities.*;
import com.keendly.mappers.MappingMode;
import com.keendly.model.*;
import com.keendly.schema.DeliveryProtos;
import com.keendly.schema.utils.Mapper;
import com.keendly.utils.ConfigUtils;
import com.keendly.utils.FeedUtils;
import org.apache.commons.lang3.RandomStringUtils;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import java.io.ByteArrayInputStream;
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

    public static int MAX_FEEDS_IN_DELIVERY = 25;
    private static int MAX_ARTICLES_IN_DELIVERY = 500;

    private static final String STATE_MACHINE_ARN = "arn:aws:states:eu-west-1:625416862388:stateMachine:Delivery3";

    private AmazonS3Client amazonS3Client = new AmazonS3Client();

    private static AWSStepFunctions awsStepFunctionsClient = getStepFunctionsClient();

    private static AWSStepFunctions getStepFunctionsClient() {
        AWSStepFunctions awsStepFunctionsClient = new AWSStepFunctionsClient();
        awsStepFunctionsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
        return awsStepFunctionsClient;
    }

    private DeliveryDao deliveryDao = new DeliveryDao();
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    public Promise<Result> createDelivery() {
        // HACK WARNING
        StringBuilder deliveryEmail = new StringBuilder();
        StringBuilder userId = new StringBuilder();
        StringBuilder provider = new StringBuilder();
        JPA.withTransaction(() -> {
            UserEntity userEntity = new UserController().lookupUser("self");
            if (userEntity.deliveryEmail != null){
                deliveryEmail.append(userEntity.deliveryEmail);
                userId.append(userEntity.id);
                provider.append(userEntity.provider.name());
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

        List<String> feedIds = delivery.items.stream().map(item -> item.feedId).collect(Collectors.toList());
        return getAdaptor().getUnread(feedIds).map(unread -> {

            int allArticles = unread.values().stream()
                    .mapToInt(Collection::size)
                    .sum();

            if (allArticles > MAX_ARTICLES_IN_DELIVERY){
                LOG.warn("More than " + MAX_ARTICLES_IN_DELIVERY + " articles found");
                unread = FeedUtils.getNewest(unread, MAX_ARTICLES_IN_DELIVERY);
            }

            boolean found = false;
            for (Map.Entry<String, List<FeedEntry>> unreadFeed : unread.entrySet()){
                if (!unreadFeed.getValue().isEmpty()){
                    found = true;
                    break;
                }
            }

            if (delivery.manual && !found){
                return badRequest(toJson(Error.NO_ARTICLES));
            } else if (!found){
                deliveryEntity.errorDescription = "NO ARTICLES";
                JPA.withTransaction(() ->  deliveryDao.createDelivery(deliveryEntity));
                LOG.warn("No items for delivery {}", deliveryEntity.id);
                return ok(Json.toJson(deliveryMapper.toModel(deliveryEntity, MappingMode.SIMPLE)));
            }

            JPA.withTransaction(() ->  deliveryDao.createDelivery(deliveryEntity));

            try {
                WorkflowType workflowType = getWorkflowType("DeliveryWorkflow.deliver", "1.3");
                if (workflowType == null){
                    LOG.error("workflow type not found");
                }
                DeliveryRequest request = Mapper.toDeliveryRequest(delivery, unread, deliveryEntity.id, deliveryEmail.toString(),
                        Long.parseLong(userId.toString()), Provider.valueOf(provider.toString()));

                request.dryRun = false;

                if (shouldRunStepFunctions(request)){
                    try {
                        StartExecutionRequest startExecutionRequest = new StartExecutionRequest();
                        startExecutionRequest.setInput(Jackson.toJsonString(request));
                        startExecutionRequest.setStateMachineArn(STATE_MACHINE_ARN);
                        StartExecutionResult result = awsStepFunctionsClient.startExecution(startExecutionRequest);
                        LOG.debug("Started step functions execution: {}", result.getExecutionArn());

                        deliveryEntity.stateMachine = STATE_MACHINE_ARN;
                        deliveryEntity.execution = result.getExecutionArn();
                        JPA.withTransaction(() -> deliveryDao.updateDelivery(deliveryEntity));
                    } catch (Exception e){
                        LOG.error("Error starting Step Functions execution", e);
                    }

                } else {
                    if (Jackson.toJsonString(request).length() > 32000){
                        String key = "messages/" + UUID.randomUUID().toString().replace("-", "") + ".json";
                        amazonS3Client.putObject("keendly", key,
                                new ByteArrayInputStream(Jackson.toJsonString(request.items).getBytes()), new ObjectMetadata());
                        request.items = null;
                        S3Object items = new S3Object();
                        items.bucket = "keendly";
                        items.key = key;
                        request.s3Items = items;
                    }
                    String workflowId = "Delivery_" + UUID.randomUUID().toString();
                    Run run = runWorkflow(workflowType, workflowId, Jackson.toJsonString(request));
                    LOG.debug("Workflow type {} version {}, started, runId: {}", workflowType.getName(),
                            workflowType.getVersion(), run.getRunId());

                    deliveryEntity.workflowId = workflowId;
                    deliveryEntity.runId = run.getRunId();
                    JPA.withTransaction(() -> deliveryDao.updateDelivery(deliveryEntity));

                    // test delivery with dryRun = true
//                    WorkflowType workflowTypeTest = getWorkflowType("DeliveryWorkflow.deliver", "1.3");
//                    request.dryRun = true;
//                    Run testRun = runWorkflow(workflowTypeTest, workflowId + "_test", Jackson.toJsonString(request));
//                    LOG.debug("TEST workflow type {} version {}, started, runId: {}", workflowTypeTest.getName(),
//                            workflowTypeTest.getVersion(), testRun.getRunId());

                    try {
                        // step functions dryRun = true
                        request.dryRun = true;

                        if (request.s3Items == null){
                            String key = "messages/" + UUID.randomUUID().toString().replace("-", "") + ".json";
                            amazonS3Client.putObject("keendly", key,
                                    new ByteArrayInputStream(Jackson.toJsonString(request.items).getBytes()), new ObjectMetadata());
                            request.items = null;
                            S3Object items = new S3Object();
                            items.bucket = "keendly";
                            items.key = key;
                            request.s3Items = items;
                        }

                        StartExecutionRequest startExecutionRequest = new StartExecutionRequest();
                        startExecutionRequest.setInput(Jackson.toJsonString(request));
                        startExecutionRequest.setStateMachineArn(STATE_MACHINE_ARN);
                        StartExecutionResult result = awsStepFunctionsClient.startExecution(startExecutionRequest);
                        LOG.debug("Started step functions execution: {}", result.getExecutionArn());
                    } catch (Exception e){
                        LOG.error("Error starting Step Functions execution", e);
                    }

                }

            } catch (Exception e){
                // catching everything for now, to avoid breaking due this
                LOG.error("Error starting SWF workflow", e);
            }

           return ok(Json.toJson(deliveryMapper.toModel(deliveryEntity, MappingMode.SIMPLE)));
        });
    }

    private static boolean shouldRunStepFunctions(DeliveryRequest request){
        if (request.email.equals("moomeen@kindle.com")){
            return true;
        }
        return false;

        //            Random generator = new Random();
//            double d = generator.nextDouble();
//            if (d <= 0.5){
        //
    }

    private static final AmazonSimpleWorkflow swfClient = getSWFClient();

    private static WorkflowType getWorkflowType(String name, String version){
        ListWorkflowTypesRequest req = new ListWorkflowTypesRequest();
        req.setDomain("keendly");
        req.setRegistrationStatus(RegistrationStatus.REGISTERED);
        for (WorkflowTypeInfo info : swfClient.listWorkflowTypes(req).getTypeInfos()){
            if (info.getWorkflowType().getName().equals(name) &&
                    info.getWorkflowType().getVersion().equals(version)){
                return info.getWorkflowType();
            }
        }

        return null;
    }

    private static Run runWorkflow(WorkflowType type, String workflowId, String input){
        StartWorkflowExecutionRequest startRequest = new StartWorkflowExecutionRequest();

        // HACKY way to imitate the way Flow Framework starts workflows
        List<String> objects = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        objects.add(input);
        parameters.add("[Ljava.lang.Object;");
        parameters.add(objects);

        startRequest.setDomain("keendly");
        startRequest.setWorkflowId(workflowId);
        startRequest.setWorkflowType(type);
        startRequest.setInput(Jackson.toJsonString(parameters));
        startRequest.setLambdaRole("arn:aws:iam::625416862388:role/swf_lambda_role");
        return swfClient.startWorkflowExecution(startRequest);
    }

    private static AmazonSimpleWorkflow getSWFClient() {
        AmazonSimpleWorkflow client = new AmazonSimpleWorkflowClient();
        client.setRegion(Region.getRegion(Regions.EU_WEST_1));
        return client;
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

    public Promise<Result> getDeliveries() {
        String subscriptionId = request().getQueryString("subscriptionId");

        List<Delivery> deliveries = new ArrayList<>();
        if (subscriptionId != null){
            JPA.withTransaction(() -> {
                List<DeliveryEntity> entities = deliveryDao.getSubscriptionDeliveries(getUserEntity(), Long.valueOf(subscriptionId));
                deliveries.addAll(deliveryMapper.toModel(entities, MappingMode.SIMPLE));
            });
        } else {
            String page = request().getQueryString("page");
            String pageSize = request().getQueryString("pageSize");
            JPA.withTransaction(() -> {
                List<DeliveryEntity> entities = deliveryDao.getDeliveries(getUserEntity(), Integer.valueOf(page), Integer.valueOf(pageSize));
                deliveries.addAll(deliveryMapper.toModel(entities, MappingMode.SIMPLE));
            });
        }

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
            delivery.created = entity.created;
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
