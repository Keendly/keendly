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
import org.apache.http.HttpStatus;
import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.net.URI;
import java.util.*;
import java.util.function.Function;

import static com.keendly.adaptors.newsblur.NewsblurAdaptor.NewsblurParam.*;
import static com.keendly.utils.ConfigUtils.parameter;

public class NewsblurAdaptor extends Adaptor {

    private static final Logger.ALogger LOG = Logger.of(NewsblurAdaptor.class);


    enum NewsblurParam {
        URL,
        CLIENT_ID,
        CLIENT_SECRET,
        REDIRECT_URL
    }

    protected Map<NewsblurParam, String> config;

    public NewsblurAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public NewsblurAdaptor(Token token){
        this(token, defaultConfig(), WS.client());
    }

    public NewsblurAdaptor(Map<NewsblurParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
    }

    public NewsblurAdaptor(Token token, Map<NewsblurParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
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
        return doGetFlat("/social/profile", response ->  {
            ExternalUser user = new ExternalUser();
            user.setId(response.get("user_id").asText());
            user.setDisplayName(response.get("user_profile").get("username").asText());
            user.setUserName(response.get("user_profile").get("username").asText());

            return get("/profile/payment_history", profile -> {
                if (profile.has("statistics") && profile.get("statistics").has("email")){
                    user.setUserName(profile.get("statistics").get("email").asText());
                }
                return user;
            }).recover(f -> user);
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
        String url = "/reader/feed/" + UrlEscapers.urlPathSegmentEscaper().escape(feedId) + "?page=" + Integer.toString(page);
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
                    FeedEntry entry = mapToFeedEntry(item);
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

    private static boolean isURL(String s){
        try {
            URI uri = new URI(s);
            if (uri.getScheme() == null || uri.getHost() == null){
                return false;
            }
            return true;
        } catch (Exception e){
            return false;
        }
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
    protected Promise<Boolean> doMarkFeedRead(List<String> feedIds, long timestamp) {
        List<Promise<WSResponse>> promises = new ArrayList<>();

        for (String feedId : feedIds){
            Promise<WSResponse> promise = client.url(config.get(URL) + "/reader/mark_feed_as_read")
                    .setHeader("Authorization", "Bearer " + token.getAccessToken())
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post("feed_id=" + feedId + "&cutoff_timestamp=" + String.valueOf(timestamp / 1000)); // to seconds

            promises.add(promise);
        }

        return Promise.sequence(promises).map(responses -> {
            for (WSResponse response : responses){
                if (response.getStatus() != HttpStatus.SC_OK){
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        });
    }

    @Override
    protected Promise<Boolean> doMarkArticleRead(List<String> articleHashes) {
        return markArticle(articleHashes, true);
    }

    @Override
    protected Promise<Boolean> doMarkArticleUnread(List<String> articleHashes) {
        return markArticle(articleHashes, false);
    }

    @Override
    protected Promise<List<FeedEntry>> doGetArticles(List<String> articleIds) {
        Map<String, List<String>> params = new HashMap<>();
        params.put("h", articleIds);

        return get("/reader/river_stories", params, response ->  {
            List<FeedEntry> feedEntries = new ArrayList<>();
            JsonNode stories = response.get("stories");
            for (JsonNode story : stories){
                FeedEntry feedEntry = mapToFeedEntry(story);
                feedEntries.add(feedEntry);
            }
            return feedEntries;
        });
    }

    private Promise<Boolean> markArticle(List<String> articleHashes, boolean asRead){
        String url = asRead ? "/reader/mark_story_hashes_as_read" : "/reader/mark_story_hash_as_unread";

        StringBuilder params = new StringBuilder();
        for (int i=0; i < articleHashes.size(); i++){
            String hash = articleHashes.get(i);
            params.append("story_hash=");
            params.append(hash);
            if (i < articleHashes.size() - 1){
                params.append("&");
            }
        }

        return client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken())
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(params.toString())
                .map(response ->
                        response.getStatus() == HttpStatus.SC_OK
                );
    }

    private Promise<WSResponse> getGetPromise(String url) {
        return getGetPromise(url, Collections.EMPTY_MAP);
    }

    private Promise<WSResponse> getGetPromise(String url, Map<String, List<String>> params) {
        WSRequest request = client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken());

        for (Map.Entry<String, List<String>> param : params.entrySet()){
            for (String val : param.getValue()){
                request.setQueryParameter(param.getKey(), val);
            }
        }
        return request.get();
    }

    protected <T> Promise<T> get(String url, Function<JsonNode, T> callback){
        return get(url, Collections.EMPTY_MAP, callback);
    }

    protected <T> Promise<T> get(String url, Map<String, List<String>> params, Function<JsonNode, T> callback){
        Promise<WSResponse> res = getGetPromise(url, params);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        JsonNode json = response.asJson();
                        if (json.has("authenticated") && !json.get("authenticated").asBoolean()){
                            throw new ApiException(401, "not authenticated");
                        } else {
                            return Promise.pure(callback.apply(json));
                        }
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doGetFlat(String url, Function<JsonNode, Promise<T>> callback){
        Promise<WSResponse> res = getGetPromise(url);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())) {
                        JsonNode json = response.asJson();
                        if (json.has("authenticated") && !json.get("authenticated").asBoolean()){
                            throw new ApiException(401, "not authenticated");
                        } else {
                            return callback.apply(json);
                        }
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private static FeedEntry mapToFeedEntry(JsonNode story){
        FeedEntry entry = new FeedEntry();
        String id = asText(story, "id");
        if (isURL(id)){
            entry.setUrl(id);
        } else {
            entry.setUrl(asText(story, "story_permalink"));
        }
        entry.setId(asText(story, "story_hash"));
        entry.setTitle(asText(story, "story_title"));
        entry.setAuthor(asText(story, "story_authors"));
        entry.setPublished(asDate(story, "story_timestamp"));
        entry.setContent(asText(story, "story_content"));
        return entry;
    }
}
