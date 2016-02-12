package adaptors.inoreader;

import adaptors.GoogleReaderTypeAdaptor;
import adaptors.exception.ApiException;
import adaptors.model.Credentials;
import adaptors.model.ExternalFeed;
import adaptors.model.ExternalUser;
import adaptors.model.Token;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    protected  <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res = getGetPromise(url);
        return res
                .flatMap(response -> {
                   if (isOk(response.getStatus())){
                       return Promise.pure(callback.apply(response));
                   } else {
                       throw new ApiException(response.getStatus(), response.getBody());
                   }
                });
    }

    @Override
    protected <T> Promise<T> getFlat(String url, Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res = getGetPromise(url);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        return callback.apply(response);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private Promise<WSResponse> getGetPromise(String url) {
        return WS.url(URL + url)
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .get();
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
        List<Promise<WSResponse>> promises = new ArrayList<>();

        for (String feedId : feedIds){
            Promise<WSResponse> promise = getGetPromise("/mark-all-as-read?s=" + feedId);
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
}
