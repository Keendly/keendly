package com.keendly.adaptors.feedly;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.FeedEntry;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.keendly.adaptors.feedly.FeedlyAdaptor.FeedlyParam.*;

public class FeedlyAdaptor extends Adaptor {

    enum FeedlyParam {
        URL,
        CLIENT_ID,
        CLIENT_SECRET,
        REDIRECT_URL
    }

    protected Map<FeedlyParam, String> config;

    public FeedlyAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public FeedlyAdaptor(Token token){
        this(token, defaultConfig(), WS.client());
    }

    public FeedlyAdaptor(Map<FeedlyParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
    }

    public FeedlyAdaptor(Token token, Map<FeedlyParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
        this.token = token;
    }


    public static Map<FeedlyParam, String> defaultConfig(){
        Map<FeedlyParam, String> config = new HashMap<>();
        config.put(URL, "feedly.url");
        config.put(CLIENT_ID, "feedly.client_id");
        config.put(CLIENT_SECRET, "feedly.client_secret");
        config.put(REDIRECT_URL, "feedly.redirect_uri");
        return config;
    }

    @Override
    public Promise<Token> doLogin(Credentials credentials){
        JsonNode json = Json.newObject()
                .put("grant_type", "authorization_code")
                .put("client_id", config.get(CLIENT_ID))
                .put("client_secret", config.get(CLIENT_SECRET))
                .put("code", credentials.getAuthorizationCode())
                .put("redirect_uri", config.get(REDIRECT_URL));

        return WS.url(config.get(URL) + "/com/keendly/auth/token")
                .post(json)
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        JsonNode node = response.asJson();
                        String refreshToken = node.get("refresh_token").asText();
                        String accessToken = node.get("access_token").asText();
                        return new Token(refreshToken, accessToken);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    public Promise<ExternalUser> doGetUser(){
        return doGet(config.get(URL) + "/profile", token, response -> {
            ExternalUser user = new ExternalUser();
            JsonNode node = response.asJson();
            user.setId(node.get("id").asText());
            user.setUserName(node.get("email").asText());
            user.setDisplayName(node.get("fullName").asText());
            return user;
        });
    }

    @Override
    public Promise<List<ExternalFeed>> doGetFeeds(){
        return doGet(config.get(URL) + "/subscriptions", token, response -> {
            List<ExternalFeed> externalSubscriptions = new ArrayList<>();
            JsonNode json = response.asJson();
            if (json.isArray()){
                for (JsonNode item : json){
                    externalSubscriptions.add(mapFromJson(item));
                }
            } else {
                externalSubscriptions.add(mapFromJson(json));
            }
            return externalSubscriptions;
        });
    }

    @Override
    public Promise<Map<String, List<FeedEntry>>> doGetUnread(List<String> feedIds){
        return doGetFlat(config.get(URL) + "/markers/counts", token, response -> {
            JsonNode json = response.asJson();
            List<Promise<Map<String, List<FeedEntry>>>> resultsPromises = new ArrayList<>();
            for (JsonNode feedCount : json.get("unreadcounts")) {
                String feedId = feedCount.get("id").asText();
                if (feedIds.contains(feedId)) {
                    int count = feedCount.get("count").asInt();
                    Promise<Map<String, List<FeedEntry>>> entries = getUnread(feedId, count, null);
                    resultsPromises.add(entries);
                }
            }

            return Promise.sequence(resultsPromises).map(ret -> {
                Map<String, List<FeedEntry>> entries = new HashMap<>();
                for (Map<String, List<FeedEntry>> entry : ret) {
                    for (Map.Entry<String, List<FeedEntry>> mapEntry : entry.entrySet()) {
                        entries.put(mapEntry.getKey(), mapEntry.getValue());
                    }
                }
                return entries;
            });
        });
    }

    @Override
    protected Promise<Map<String, Integer>> doGetUnreadCount(List<String> feedIds) {
        return doGet(config.get(URL) + "/markers/counts", token, response -> {
            JsonNode json = response.asJson();
            Map<String, Integer> unreadCount = new HashMap<>();
            for (JsonNode feedCount : json.get("unreadcounts")) {
                String feedId = feedCount.get("id").asText();
                if (feedIds.contains(feedId)) {
                    int count = feedCount.get("count").asInt();
                    unreadCount.put(feedId, count);
                }
            }
            return unreadCount;
        });
    }

    private Promise<Map<String, List<FeedEntry>>> getUnread(String feedId, int unreadCount, String continuation) {
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url = config.get(URL) + "/streams/" + urlEncode(feedId) + "/contents";
        url = continuation == null ? url : url + "?continuation=" + continuation;
        return doGetFlat(url, token,
                response -> {
                    List<FeedEntry> entries = new ArrayList<>();
                    for (JsonNode item : response.asJson().get("items")) {
                        if (item.get("unread").asBoolean()) {
                            String articleUrl = null;
                            String originId = item.get("originId").asText();
                            if (isURL(originId)) {
                                articleUrl = originId;
                            } else {
                                for (JsonNode alternate : item.get("alternate")) {
                                    if (alternate.get("type").asText().equals("text/html")) {
                                        articleUrl = alternate.get("href").asText();
                                    }
                                }
                            }
                            if (articleUrl != null) {
                                FeedEntry entry = new FeedEntry();
                                entry.setUrl(articleUrl);
                                entry.setTitle(asText(item, "title"));
                                entry.setAuthor(asText(item, "author"));
                                entry.setPublished(asDate(item, "published"));
                                entry.setContent(extractContent(item));
                                entries.add(entry);
                            }
                            if (entries.size() >= count) {
                                break;
                            }
                        }
                    }
                    Map<String, List<FeedEntry>> ret = new HashMap<>();
                    ret.put(feedId, entries);
                    if (ret.size() < count) {
                        JsonNode continuationNode = response.asJson().get("continuation");
                        boolean hasContinuation = continuationNode != null;
                        if (hasContinuation){
                            Promise<Map<String, List<FeedEntry>>> nextPagePromise =
                                    getUnread(feedId, count - ret.get(feedId).size(), continuationNode.asText());
                            return nextPagePromise.map(nextPage -> {
                                ret.get(feedId).addAll(nextPage.get(feedId));
                                return ret;
                            });
                        }
                    }
                    return Promise.pure(ret);
                });
    }

    private String extractContent(JsonNode item){
        if (item.get("content") != null && item.get("content").get("content") != null){
            return item.get("content").get("content").asText();
        } else if (item.get("summary") != null && item.get("summary").get("content") != null){
            return item.get("summary").get("content").asText();
        }
        return null;
    }

    @Override
    public Promise doMarkAsRead(List<String> feedIds, long timestamp){
        // TODO
        Map<String, Object> data = new HashMap<>();
        data.put("action", "markAsRead");
        data.put("feedIds", feedIds);
        data.put("asOf", String.valueOf(System.currentTimeMillis()));
        data.put("type", "items");

        JsonNode content = Json.toJson(data);
        return doPost(config.get(URL) + "/markers", token, content, response -> "OK");
    }

    private static String urlEncode(String s){
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isURL(String s){
        try {
            new URL(s);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private ExternalFeed mapFromJson(JsonNode json){
        ExternalFeed externalSubscription = new ExternalFeed();
        externalSubscription.setFeedId(json.get("id").asText());
        externalSubscription.setTitle(json.get("title").asText());
        return externalSubscription;
    }

    private <T> Promise<T> doPost(String url, Token tokens, JsonNode content, Function<WSResponse, T> callback){
        Promise<WSResponse> res = WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .setContentType("application/json")
                .post(content);
        return res
                .flatMap(response -> {
                    int status = response.getStatus();
                    if (isOk(status)) {
                        return Promise.pure(callback.apply(response));
                    } else if (isUnauthorized(status)) {
                        Promise token = refreshAccessToken(tokens.getRefreshToken());
                        return token.flatMap(newToken -> {
                            tokens.setAccessToken((String) newToken);
                            tokens.setRefreshed();
                            return doPostNoRefresh(url, tokens, content, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doPostNoRefresh(String url, Token token, JsonNode content,
                                           Function<WSResponse, T> callback) {
        return WS.url(url)
                .setHeader("Authorization", "OAuth " + token.getAccessToken())
                .setContentType("application/json")
                .post(content)
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        return callback.apply(response);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doGet(String url, Token tokens, Function<WSResponse, T> callback){
        Promise<WSResponse> res = WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get();
        return res
                .flatMap(response -> {
                    int status = response.getStatus();
                    if (isOk(status)) {
                        return Promise.pure(callback.apply(response));
                    } else if (isUnauthorized(status)) {
                        Promise token = refreshAccessToken(tokens.getRefreshToken());
                        return token.flatMap(newToken -> {
                            tokens.setAccessToken((String) newToken);
                            tokens.setRefreshed();
                            return doGetNoRefresh(url, tokens, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doGetFlat(String url, Token tokens, Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res = WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get();
        return res
                .flatMap(response -> {
                    int status = response.getStatus();
                    if (isOk(status)) {
                        return callback.apply(response);
                    } else if (isUnauthorized(status)) {
                        Promise token = refreshAccessToken(tokens.getRefreshToken());
                        return token.flatMap(newToken -> {
                            tokens.setAccessToken((String) newToken);
                            tokens.setRefreshed();
                            return doGetNoRefresh(url, tokens, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doGetNoRefresh(String url, Token token, Function<WSResponse, T> callback){
        return WS.url(url)
                .setHeader("Authorization", "OAuth " + token.getAccessToken())
                .get()
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        return callback.apply(response);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private Promise<String> refreshAccessToken(String refreshToken){
        JsonNode json = Json.newObject()
                .put("grant_type", "refresh_token")
                .put("client_id", config.get(CLIENT_ID))
                .put("client_secret", config.get(CLIENT_SECRET))
                .put("refresh_token", refreshToken);

        return WS.url(config.get(URL) + "/com/keendly/auth/token")
                .post(json)
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        JsonNode node = response.asJson();
                        return node.get("access_token").asText();
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }
}
