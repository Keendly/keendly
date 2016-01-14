package adaptors.feedly;

import adaptors.Adaptor;
import adaptors.exception.ApiException;
import adaptors.model.Entry;
import adaptors.model.SubscribedFeed;
import auth.Token;
import adaptors.model.ExternalUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;

import static utils.ConfigUtils.parameter;

public class FeedlyAdaptor extends Adaptor {

    private static final int MAX_ARTICLES_PER_FEED = 100;

    enum FeedlyParam {
        URL("feedly.url"),
        CLIENT_ID("feedly.client_id"),
        CLIENT_SECRET("feedly.client_secret"),
        REDIRECT_URL("feedly.redirect_uri");

        String value;
        FeedlyParam(String value){
            this.value = value;
        }
    }

    private static String feedlyUrl;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;

    static {
        init();
    }

    private static void init(){
        feedlyUrl = parameter(FeedlyParam.URL.value);
        clientId = parameter(FeedlyParam.CLIENT_ID.value);
        clientSecret = parameter(FeedlyParam.CLIENT_SECRET.value);
        redirectUri = parameter(FeedlyParam.REDIRECT_URL.value);
    }

    @Override
    public Promise<Token> login(String authorizationCode){
        JsonNode json = Json.newObject()
                .put("grant_type", "authorization_code")
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("code", authorizationCode)
                .put("redirect_uri", redirectUri);

        return WS.url(feedlyUrl + "/auth/token")
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
    public Promise<ExternalUser> getUser(Token token){
        return doGet(feedlyUrl + "/profile", token, response -> {
            ExternalUser user = new ExternalUser();
            JsonNode node = response.asJson();
            user.setId(node.get("id").asText());
            user.setUserName(node.get("email").asText());
            user.setDisplayName(node.get("fullName").asText());
            return user;
        });
    }

    @Override
    public Promise<List<SubscribedFeed>> getSubscribedFeeds(Token token){
        return doGet(feedlyUrl + "/subscriptions", token, response -> {
            List<SubscribedFeed> externalSubscriptions = new ArrayList<>();
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
    public Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Token token){
        return doGetFlat(feedlyUrl + "/markers/counts", token, response -> {
            JsonNode json = response.asJson();
            List<Promise<Map<String, List<Entry>>>> resultsPromises = new ArrayList<>();
            for (JsonNode feedCount : json.get("unreadcounts")) {
                String feedId = feedCount.get("id").asText();
                if (feedIds.contains(feedId)) {
                    int count = feedCount.get("count").asInt();
                    Promise<Map<String, List<Entry>>> entries = getUnread(feedId, count, token, null);
                    resultsPromises.add(entries);
                }
            }

            return Promise.sequence(resultsPromises).map(ret -> {
                Map<String, List<Entry>> entries = new HashMap<>();
                for (Map<String, List<Entry>> entry : ret) {
                    for (Map.Entry<String, List<Entry>> mapEntry : entry.entrySet()) {
                        entries.put(mapEntry.getKey(), mapEntry.getValue());
                    }
                }
                return entries;
            });
        });
    }

    private Promise<Map<String, List<Entry>>> getUnread(String feedId, int unreadCount, Token token, String continuation) {
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url = feedlyUrl + "/streams/" + urlEncode(feedId) + "/contents";
        url = continuation == null ? url : url + "?continuation=" + continuation;
        return doGetFlat(url, token,
                response -> {
                    List<Entry> entries = new ArrayList<>();
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
                                Entry entry = new Entry();
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
                    Map<String, List<Entry>> ret = new HashMap<>();
                    ret.put(feedId, entries);
                    if (ret.size() < count) {
                        JsonNode continuationNode = response.asJson().get("continuation");
                        boolean hasContinuation = continuationNode != null;
                        if (hasContinuation){
                            Promise<Map<String, List<Entry>>> nextPagePromise =
                                    getUnread(feedId, count - ret.get(feedId).size(), token, continuationNode.asText());
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

    public Promise markAsRead(List<String> feedIds, Token token){
        Map<String, Object> data = new HashMap<>();
        data.put("action", "markAsRead");
        data.put("feedIds", feedIds);
        data.put("asOf", String.valueOf(System.currentTimeMillis()));
        data.put("type", "items");

        JsonNode content = Json.toJson(data);
        return doPost(feedlyUrl + "/markers", token, content, response -> "OK");
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

    private static String asText(JsonNode node, String field){
        JsonNode j = node.get(field);
        if (j != null){
            return j.asText();
        }
        return null;
    }

    private static Date asDate(JsonNode node, String field){
        JsonNode j = node.get(field);
        if (j != null){
            Date d = new Date();
            d.setTime(j.asLong());
            return d;
        }
        return null;
    }

    private SubscribedFeed mapFromJson(JsonNode json){
        SubscribedFeed externalSubscription = new SubscribedFeed();
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
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("refresh_token", refreshToken);

        return WS.url(feedlyUrl + "/auth/token")
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

    private boolean isOk(int status){
        return status == HttpStatus.SC_OK;
    }

    private boolean isUnauthorized(int status){
        if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN){
            return true;
        }
        return false;
    }
}
