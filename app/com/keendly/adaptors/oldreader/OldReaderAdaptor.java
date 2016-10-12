package com.keendly.adaptors.oldreader;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.GoogleReaderMapper;
import com.keendly.adaptors.GoogleReaderTypeAdaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import org.apache.commons.lang3.StringUtils;
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

import static com.keendly.adaptors.oldreader.OldReaderAdaptor.OldReaderParam.AUTH_URL;
import static com.keendly.adaptors.oldreader.OldReaderAdaptor.OldReaderParam.URL;
import static com.keendly.utils.ConfigUtils.parameter;

public class OldReaderAdaptor extends GoogleReaderTypeAdaptor {

    private static final String APP_NAME = "Keendly";

    enum OldReaderParam {
        URL,
        AUTH_URL
    }

    protected Map<OldReaderParam, String> config;

    public OldReaderAdaptor(){
        this(defaultConfig(), WS.client());
    }

    public OldReaderAdaptor(Token token){
        this(token, defaultConfig(), WS.client());
    }

    public OldReaderAdaptor(Map<OldReaderParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
    }

    public OldReaderAdaptor(Token token, Map<OldReaderParam, String> config, WSClient client){
        this.client = client;
        this.config = config;
        this.token = token;
    }

    public static Map<OldReaderParam, String> defaultConfig(){
        Map<OldReaderParam, String> config = new HashMap<>();
        config.put(URL, parameter("oldreader.url"));
        config.put(AUTH_URL, parameter("oldreader.auth_url"));
        return config;
    }

    @Override
    protected Promise<Token> doLogin(Credentials credentials) {
        return client.url(config.get(AUTH_URL))
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format(
                        "client=%s&accountType=HOSTED_OR_GOOGLE&service=reader&Email=%s&Passwd=%s",
                        APP_NAME, credentials.getUsername(), credentials.getPassword()
                ))
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        String token = extractToken(response.asByteArray());
                        if (token == null){
                            throw new ApiException(HttpStatus.SC_SERVICE_UNAVAILABLE);
                        }
                        return new Token(token);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private String extractToken(byte[] response){
        String s = new String(response);
        String[] params = s.split("\n");
        for (String param : params){
            String[] p = param.split("=");
            if (p.length == 2 && p[0].equals("Auth")){
                return p[1];
            }
        }
        return null;
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
    protected Promise<Boolean> doMarkAsRead(List<String> feedIds, long timestamp) {
        List<Promise<WSResponse>> promises = new ArrayList<>();

        for (String feedId : feedIds){
            Promise<WSResponse> promise = client.url(config.get(URL) + "/mark-all-as-read")
                    .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post("s=" + feedId + "&ts=" + String.valueOf(timestamp * 1000000)); // to nanoseconds

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
    protected <T> Promise<T> get(String url, Map<String, String> params, Function<WSResponse, T> callback){
        Promise<WSResponse> res =  client.url(config.get(URL) + normalizeURL(url))
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

    @Override
    protected <T> Promise<T> getFlat(String url, Map<String, String> params,
                                     Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res =  client.url(config.get(URL) + normalizeURL(url))
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
    protected <T> Promise<T> post(String url, Map<String, String> params, Function<WSResponse, T> callback) {
        Promise<WSResponse> res =  client.url(config.get(URL) + normalizeURL(url))
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .post(StringUtils.EMPTY);
        return res
                .flatMap(response -> {
                    if (isOk(response.getStatus())){
                        return Promise.pure(callback.apply(response));
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private static String normalizeURL(String url){
        if (url.contains("?")){
            return url + "&output=json";
        } else {
            return url + "?output=json";
        }
    }
}
