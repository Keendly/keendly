package com.keendly.adaptors.newsblur;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.UrlEscapers;
import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.FeedEntry;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.util.*;
import java.util.function.Function;

import static com.keendly.adaptors.newsblur.NewsblurAdaptor.NewsblurParam.*;
import static com.keendly.utils.ConfigUtils.parameter;

public class NewsblurAdaptor extends Adaptor {

    enum NewsblurParam {
        URL,
        CLIENT_ID,
        CLIENT_SECRET,
        REDIRECT_URL
    }

    private Map<NewsblurParam, String> config;
    private WSClient client;

    public NewsblurAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public NewsblurAdaptor(Map<NewsblurParam, String> config, WSClient client){
        this.config = config;
        this.client = client;
    }

    public NewsblurAdaptor(Token token) {
        this(token, defaultConfig(), WS.client());
    }

    public NewsblurAdaptor(Token token, Map<NewsblurParam, String> config, WSClient client) {
        this(config, client);
        this.token = token;
    }

    private static Map<NewsblurParam, String> defaultConfig(){
        Map<NewsblurParam, String> config = new HashMap<>();
        config.put(URL, parameter("newsblur.url"));
        config.put(CLIENT_ID, parameter("newsblur.client_id"));
        config.put(CLIENT_SECRET, parameter("newsblur.client_secret"));
        config.put(REDIRECT_URL, parameter("newsblur.redirect_uri"));

        return config;
    }

    @Override
    protected Promise<Token> doLogin(Credentials credentials) {
        return client.url(config.get(URL) + "/oauth/token")
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format("code=%s&" +
                                "redirect_uri=%s&" +
                                "client_id=%s&" +
                                "client_secret=%s" +
                                "&grant_type=authorization_code",
                        credentials.getAuthorizationCode(),
                        config.get(REDIRECT_URL), UrlEscapers.urlFormParameterEscaper().escape(config.get(CLIENT_ID)),
                        UrlEscapers.urlFormParameterEscaper().escape(config.get(CLIENT_SECRET))))
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
    protected Promise<ExternalUser> doGetUser() {
        return get("/social/profile", response ->  {
            ExternalUser user = new ExternalUser();
            user.setId(response.get("user_id").asText());
            user.setDisplayName(response.get("user_profile").get("username").asText());
            user.setUserName(response.get("user_profile").get("username").asText());
            return user;
        });
    }

    @Override
    protected Promise<List<ExternalFeed>> doGetFeeds() {
        return get("/reader/feeds", response ->  {
            List<ExternalFeed> externalSubscriptions = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = response.get("feeds").fields();
            while (it.hasNext()){
                Map.Entry<String, JsonNode> feed = it.next();
                ExternalFeed externalSubscription = new ExternalFeed();
                externalSubscription.setFeedId(feed.getValue().get("id").asText());
                externalSubscription.setTitle(feed.getValue().get("feed_title").asText());
                externalSubscriptions.add(externalSubscription);
            }
            return externalSubscriptions;
        });
    }

    @Override
    protected Promise<Map<String, List<FeedEntry>>> doGetUnread(List<String> feedIds) {
        return getUnreadCount(feedIds).flatMap(unreadCounts -> {
            List<F.Promise<Map<String, List<FeedEntry>>>> resultsPromises = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unreadCounts.entrySet()){
                F.Promise<Map<String, List<FeedEntry>>> entries = doGetUnread(entry.getKey(), entry.getValue(), 1);
                resultsPromises.add(entries);
            }

            return F.Promise.sequence(resultsPromises).map(ret -> {
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

    private F.Promise<Map<String, List<FeedEntry>>> doGetUnread(String feedId, int unreadCount, int page) {
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url = "/reader/feed/" + feedId + "?page=" + Integer.toString(page);
        Promise<WSResponse> res = getGetPromise(url);
        return res.flatMap(response -> {
            JsonNode jsonResponse = response.asJson();
            JsonNode items = jsonResponse.get("stories");
            if (items == null){
                return F.Promise.pure(Collections.emptyMap());
            }
            List<FeedEntry> entries = new ArrayList<>();
            for (JsonNode item : items){
                if (item.get("read_status").asInt() == 0){
                    FeedEntry entry = new FeedEntry();
                    entry.setUrl(asText(item, "id"));
                    entry.setTitle(asText(item, "story_title"));
                    entry.setAuthor(asText(item, "story_authors"));
                    entry.setPublished(asDate(item, "story_timestamp"));
                    entry.setContent(asText(item, "story_content"));
                    entries.add(entry);
                }
            }

            Map<String, List<FeedEntry>> ret = new HashMap<>();
            ret.put(feedId, entries);
            if (ret.get(feedId).size() < count){
                F.Promise<Map<String, List<FeedEntry>>> nextPagePromise =
                        doGetUnread(feedId, count - ret.get(feedId).size(), page+1);
                return nextPagePromise.map(nextPage -> {
                    ret.get(feedId).addAll(nextPage.get(feedId));
                    return ret;
                });
            }

            return F.Promise.pure(ret);

        });
    }

    @Override
    protected Promise<Map<String, Integer>> doGetUnreadCount(List<String> feedIds) {
        return get("/reader/refresh_feeds", response ->  {
            Map<String, Integer> unreadCount = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = response.get("feeds").fields();
            while (it.hasNext()){
                Map.Entry<String, JsonNode> feed = it.next();
                if (feedIds.contains(feed.getKey())){
                    unreadCount.put(feed.getKey(), feed.getValue().get("nt").asInt());
                }
            }
            return unreadCount;
        });
    }

    @Override
    protected Promise doMarkAsRead(List<String> feedIds) {
        return null;
    }

    private Promise<WSResponse> getGetPromise(String url) {
        return client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken())
                .get();
    }

    protected  <T> Promise<T> get(String url, Function<JsonNode, T> callback){
        Promise<WSResponse> res = getGetPromise(url);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        JsonNode json = response.asJson();
                        if (json.has("authenticated") && !json.get("authenticated").asBoolean()){
                            throw new ApiException(401, "not authenticated");
                        } else {
                            return Promise.pure(callback.apply(json));
                        }
//                    } else if (isUnauthorized(response.getStatus())){
//                        Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
//                        return refreshedToken.flatMap(newToken -> {
//                            token.setAccessToken((String) newToken);
//                            token.setRefreshed();
//                            return doGetNoRefresh(url, callback);
//                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }
}
