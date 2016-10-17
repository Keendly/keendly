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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.*;
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

    protected Map<InoreaderParam, String> config;

    public InoreaderAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public InoreaderAdaptor(Token token){
        this(token, defaultConfig(), WS.client());
    }

    public InoreaderAdaptor(Map<InoreaderParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
    }

    public InoreaderAdaptor(Token token, Map<InoreaderParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
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
    protected  <T> Promise<T> get(String url, Map<String, String> params, Function<WSResponse, T> callback){
        Promise<WSResponse> res = getGetPromise(url, params);
        return res
                .flatMap(response -> {
                   if (isOk(response.getStatus())){
                       return Promise.pure(callback.apply(response));
                   } else if (isUnauthorized(response.getStatus())){
                       Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
                       return refreshedToken.flatMap(newToken -> {
                           token.setAccessToken((String) newToken);
                           token.setRefreshed();
                           return doGetNoRefresh(url, params, callback);
                       });
                   } else {
                       throw new ApiException(response.getStatus(), response.getBody());
                   }
                });
    }

    protected  <T> Promise<T> post(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res = getPostPromise(url);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        return Promise.pure(callback.apply(response));
                    } else if (isUnauthorized(response.getStatus())){
                        Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
                        return refreshedToken.flatMap(newToken -> {
                            token.setAccessToken((String) newToken);
                            token.setRefreshed();
                            return doPostNoRefresh(url, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    protected <T> Promise<T> getFlat(String url, Map<String, String> params,
                                     Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res = getGetPromise(url, params);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())) {
                        return callback.apply(response);
                    } else if (isUnauthorized(response.getStatus())){
                        Promise refreshedToken = refreshAccessToken(token.getRefreshToken());
                        return refreshedToken.flatMap(newToken -> {
                            token.setAccessToken((String) newToken);
                            token.setRefreshed();
                            return doGetNoRefresh(url, params, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private Promise<WSResponse> getGetPromise(String url, Map<String, String> params) {
        WSRequest req = client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken());
        for (Map.Entry<String, String> param : params.entrySet()){
            req.setQueryParameter(param.getKey(), param.getValue());
        }
        return req.get();
    }

    private Promise<WSResponse> getPostPromise(String url) {
        WSRequest req = client.url(config.get(URL) + url)
                .setHeader("Authorization", "Bearer " + token.getAccessToken());

        return req.post(StringUtils.EMPTY);
    }

    private <T> Promise<T> doGetNoRefresh(String url, Map<String, String> params,
                                          Function<WSResponse, T> callback){
        Promise<WSResponse> res = getGetPromise(url, params);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        return Promise.pure(callback.apply(response));
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doPostNoRefresh(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res = getPostPromise(url);
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
    protected Promise doMarkFeedRead(List<String> feedIds, long timestamp) {
        List<Promise<WSResponse>> promises = new ArrayList<>();

        for (String feedId : feedIds){
            Map<String, String> params = new HashMap<>();
            params.put("s", feedId);
            params.put("ts", Long.toString(timestamp * 1000));
            Promise<WSResponse> promise = getGetPromise("/mark-all-as-read", params);
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
                    } else if (isInvalidRefreshToken(response.getStatus(), response.getBody())){
                        throw new ApiException(HttpStatus.SC_UNAUTHORIZED, response.getBody());
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    protected static boolean isInvalidRefreshToken(int status, String body){
        if (status == HttpStatus.SC_BAD_REQUEST && body.contains("Invalid refresh token")){
            return true;
        }
        return false;
    }

    @Override
    protected F.Promise<Boolean> doMarkArticleRead(List<String> articleIds){
        return editTag(true, "user/-/state/com.google/read", articleIds);
    }

    @Override
    protected F.Promise<Boolean> doMarkArticleUnread(List<String> articleIds){
        return editTag(false, "user/-/state/com.google/read", articleIds);
    }

    private F.Promise<Boolean> editTag(boolean add, String tag, List<String> ids){
        String action = add ? "a" : "r";
        String url = "/edit-tag?" + action + "=" + tag;
        for (String id : ids){
            url = url + "&i=" + id;
        }

        return post(url, response -> {
            if (response.getStatus() != HttpStatus.SC_OK){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        });
    }
}
