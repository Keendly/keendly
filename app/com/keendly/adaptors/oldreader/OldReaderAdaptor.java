package com.keendly.adaptors.oldreader;

import com.keendly.adaptors.GoogleReaderTypeAdaptor;
import com.keendly.adaptors.exception.ApiException;
import com.keendly.adaptors.model.*;
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

public class OldReaderAdaptor extends GoogleReaderTypeAdaptor {

    private static final String URL = "https://theoldreader.com/reader/api/0";
    private static final String APP_NAME = "Keendly";

    public OldReaderAdaptor(){

    }

    public OldReaderAdaptor(Token token){
        super(token);
    }

    @Override
    protected Promise<Token> doLogin(Credentials credentials) {
        return WS.url("https://theoldreader.com/accounts/ClientLogin")
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(String.format(
                        "%s&accountType=HOSTED_OR_GOOGLE&service=reader&Email=%s&Passwd=%s",
                        APP_NAME, credentials.getUsername(), credentials.getPassword()
                ))
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
    protected Promise<ExternalUser> doGetUser() {
        return get("/user-info", response -> toUser(response.asJson()));
    }

    @Override
    protected Promise<List<ExternalFeed>> doGetFeeds() {
        return get("/subscription/list", response ->  toFeeds(response.asJson()));
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
    protected Promise<Boolean> doMarkAsRead(List<String> feedIds) {
        List<Promise<WSResponse>> promises = new ArrayList<>();

        for (String feedId : feedIds){
            Map<String, Object> data = new HashMap<>();
            data.put("s", feedId);
            data.put("ts", String.valueOf(System.currentTimeMillis()));

            Promise<WSResponse> promise = WS.url(URL + "/mark-all-as-read")
                    .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post("s=" + feedId + "&ts=" + String.valueOf(System.currentTimeMillis()));

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
    protected <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res =  WS.url(URL + normalizeURL(url))
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
    protected <T> Promise<T> getFlat(String url, Function<WSResponse, Promise<T>> callback){
        Promise<WSResponse> res =  WS.url(URL + normalizeURL(url))
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

    private static String normalizeURL(String url){
        if (url.contains("?")){
            return url + "&output=json";
        } else {
            return url + "?output=json";
        }
    }
}