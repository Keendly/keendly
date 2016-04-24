package com.keendly.adaptors.inoreader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.UrlEscapers;
import com.keendly.adaptors.GoogleReaderMapper;
import com.keendly.adaptors.GoogleReaderTypeAdaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.keendly.adaptors.inoreader.InoreaderAdaptor.InoreaderParam.*;
import static com.keendly.utils.ConfigUtils.parameter;

public class InoreaderAdaptor extends GoogleReaderTypeAdaptor {

    enum InoreaderParam {
        URL,
        AUTH_URL,
        CLIENT_ID,
        CLIENT_SECRET,
        REDIRECT_URL
    }

    private Map<InoreaderParam, String> config;
    private WSClient client;

    public InoreaderAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public InoreaderAdaptor(Map<InoreaderParam, String> config, WSClient client){
        this.config = config;
        this.client = client;
    }

    public InoreaderAdaptor(Token token) {
        this(token, defaultConfig(), WS.client());
    }

    public InoreaderAdaptor(Token token, Map<InoreaderParam, String> config, WSClient client) {
        this(config, client);
        this.token = token;
    }

    private static Map<InoreaderParam, String> defaultConfig(){
        Map<InoreaderParam, String> config = new HashMap<>();
        config.put(URL, parameter("inoreader.url"));
        config.put(AUTH_URL, parameter("inoreader.auth_url"));
        config.put(CLIENT_ID, parameter("inoreader.client_id"));
        config.put(CLIENT_SECRET, parameter("inoreader.client_secret"));
        config.put(REDIRECT_URL, parameter("inoreader.redirect_uri"));

        return config;
    }

    @Override
    protected Promise<Token> doLogin(Credentials credentials) {
        return client.url(config.get(AUTH_URL))
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format("code=%s&" +
                        "redirect_uri=%s&" +
                        "client_id=%s&" +
                        "client_secret=%s" +
                        "&scope=write" +
                        "&grant_type=authorization_code",
                        credentials.getAuthorizationCode(),
                        config.get(REDIRECT_URL), config.get(CLIENT_ID), config.get(CLIENT_SECRET)))
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
        return get("/user-info", response -> GoogleReaderMapper.toUser(response.asJson()));
    }

    @Override
    protected Promise<List<ExternalFeed>> doGetFeeds() {
        return get("/subscription/list", response ->  GoogleReaderMapper.toFeeds(response.asJson()));
    }

    @Override
    protected  <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res = getGetPromise(url);
        return res
                .flatMap(response -> {
                   if (isOk(response.getStatus())){
                       return Promise.pure(callback.apply(response));
                   } else if (isUnauthorized(response.getStatus())){
                       Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
                       return refreshedToken.flatMap(newToken -> {
                           token.setAccessToken((String) newToken);
                           token.setRefreshed();
                           return doGetNoRefresh(url, callback);
                       });
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
                    if (isOk(response.getStatus())) {
                        return callback.apply(response);
                    } else if (isUnauthorized(response.getStatus())){
                        Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
                        return refreshedToken.flatMap(newToken -> {
                            token.setAccessToken((String) newToken);
                            token.setRefreshed();
                            return doGetNoRefresh(url, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private Promise<WSResponse> getGetPromise(String url) {
        return client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken())
                .get();
    }

    private <T> Promise<T> doGetNoRefresh(String url, Function<WSResponse, T> callback){
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
    protected Promise doMarkAsRead(List<String> feedIds) {
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

    private Promise<String> refreshAccessToken(String refreshToken){
        return client.url(config.get(AUTH_URL))
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format("client_id=%s&" +
                                "client_secret=%s" +
                                "&grant_type=refresh_token" +
                                "&refresh_token=%s",
                        config.get(CLIENT_ID), config.get(CLIENT_SECRET), refreshToken))
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
