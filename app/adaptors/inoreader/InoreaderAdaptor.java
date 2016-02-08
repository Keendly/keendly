package adaptors.inoreader;

import adaptors.GoogleReaderTypeAdaptor;
import adaptors.exception.ApiException;
import adaptors.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import entities.Provider;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;

public class InoreaderAdaptor extends GoogleReaderTypeAdaptor {

    private static final String URL = "https://www.inoreader.com/reader/api/0";
    private static final String APP_ID = "1000001083";
    private static final String APP_KEY = "LiFY_ZeWCm70HT62kN17wnQlki3BjJtX";

    public InoreaderAdaptor(){

    }

    public InoreaderAdaptor(Token token) {
        super(token);
    }

    @Override
    public Promise<Token> doLogin(Credentials credentials) {
        return WS.url("https://www.inoreader.com/accounts/ClientLogin")
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format("Email=%s&Passwd=%s", credentials.getUsername(), credentials.getPassword()))
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        String token = extractToken(response.asByteArray());
                        if (token == null){
                            throw new ApiException(HttpStatus.SC_SERVICE_UNAVAILABLE);
                        }
                        return new Token(null, token); // HACK
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    public Promise<ExternalUser> doGetUser() {
        return get("/user-info", response -> toUser(response.asJson()));
    }

    @Override
    public Promise<List<ExternalFeed>> doGetFeeds() {
        return get("/subscription/list", response ->  toFeeds(response.asJson()));
    }

    private <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res =  WS.url(URL + url)
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .get();
        return res
                .flatMap(response -> {
                   if (isOk(response.getStatus())){
                       return Promise.pure(callback.apply(response));
                   } else {
                       throw new ApiException(response.getStatus(), response.getBody());
                   }
                });
    }

    private <T> Promise<T> getFlat(String url, Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res =  WS.url(URL + url)
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .get();
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        return callback.apply(response);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    public Promise<Map<String, List<Entry>>> doGetUnread(List<String> feedIds) {
        return getUnreadCount(feedIds).flatMap(unreadCounts -> {
            List<Promise<Map<String, List<Entry>>>> resultsPromises = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unreadCounts.entrySet()){
                Promise<Map<String, List<Entry>>> entries = doGetUnread(entry.getKey(), entry.getValue(), null);
                resultsPromises.add(entries);
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

    private Promise<Map<String, List<Entry>>> doGetUnread(String feedId, int unreadCount, String continuation){
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url ="/stream/contents/" + URLEncoder.encode(feedId) + "?xt=user/-/state/com.google/read";
        url = continuation == null ? url : url + "&continuation=" + continuation;
        return getFlat(url, response -> {
            JsonNode items = response.asJson().get("items");
            if (items == null){
                return Promise.pure(Collections.emptyMap());
            }
            List<Entry> entries = new ArrayList<>();
            for (JsonNode item : items){
                Entry entry = new Entry();
                entry.setUrl(extractArticleUrl(item));
                entry.setTitle(asText(item, "title"));
                entry.setAuthor(asText(item, "author"));
                entry.setPublished(asDate(item, "published"));
                entry.setContent(extractContent(item));
                entries.add(entry);
            }
            Map<String, List<Entry>> ret = new HashMap<>();
            ret.put(feedId, entries);
            if (ret.size() < count){
                JsonNode continuationNode = response.asJson().get("continuation");
                boolean hasContinuation = continuationNode != null;
                if (hasContinuation){
                    Promise<Map<String, List<Entry>>> nextPagePromise =
                            doGetUnread(feedId, count - ret.get(feedId).size(), continuationNode.asText());
                    return nextPagePromise.map(nextPage -> {
                        ret.get(feedId).addAll(nextPage.get(feedId));
                        return ret;
                    });
                }
            }

            return Promise.pure(ret);
        });
    }

    private String extractArticleUrl(JsonNode node){
        if (node.get("alternate") != null){
            for (JsonNode alternate : node.get("alternate")) {
                if (alternate.get("type").asText().equals("text/html")) {
                    return alternate.get("href").asText();
                }
            }
        } else if (node.get("canonical") != null) {
            for (JsonNode canonical : node.get("canonical")) {
                return canonical.get("href").asText();
            }
        }
        return null;
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
    protected Promise<Map<String, Integer>> doGetUnreadCount(List<String> feedIds) {
        Map<String, Integer> unreadCount = new HashMap<>();
        return get("/unread-count", response -> {
            JsonNode node = response.asJson();
            for (JsonNode unread : node.get("unreadcounts")){
                if (feedIds.contains(unread.get("id").asText())){
                    unreadCount.put(unread.get("id").asText(), unread.get("count").asInt());
                }
            }
            return unreadCount;
        });
    }

    @Override
    public Promise doMarkAsRead(List<String> feedIds) {
        throw new NotImplementedException("pim");
    }

    @Override
    public Provider getProvider() {
        return Provider.INOREADER;
    }
}
