package com.keendly.adaptors.inoreader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.UrlEscapers;
import com.keendly.adaptors.GoogleReaderTypeAdaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.keendly.utils.ConfigUtils.parameter;

public class InoreaderAdaptor extends GoogleReaderTypeAdaptor {

    private static final String URL = "https://www.inoreader.com/reader/api/0";
    private static final String CLIENT_ID = parameter("inoreader.client_id");
    private static final String CLIENT_SECRET = parameter("inoreader.client_secret");
    private static final String REDIRECT_URL = parameter("inoreader.redirect_uri");

    public InoreaderAdaptor(){

    }

    public InoreaderAdaptor(Token token) {
        super(token);
    }

    @Override
    public Promise<Token> doLogin(Credentials credentials) {
        return WS.url("https://www.inoreader.com/oauth2/token")
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format("code=%s&" +
                        "redirect_uri=%s&" +
                        "client_id=%s&" +
                        "client_secret=%s" +
                        "&scope=write" +
                        "&grant_type=authorization_code",
                        credentials.getAuthorizationCode(), REDIRECT_URL, CLIENT_ID, CLIENT_SECRET))
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        JsonNode node = response.asJson();
                        String refreshToken = node.get("refresh_token").asText();
                        String accessToken = node.get("access_token").asText();
                        int tokenExpiresIn = node.get("expires_in").asInt();
                        DateTime expirationDate =
                            DateTime.now().plus(Seconds.seconds(tokenExpiresIn));

                        return new Token(refreshToken, accessToken, expirationDate);
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
                .setHeader("Authorization", "Bearer " + token.getAccessToken())
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

    @Override
    protected String normalizeFeedId(String feedId){
        return UrlEscapers.urlPathSegmentEscaper().escape(feedId);
    }
}
