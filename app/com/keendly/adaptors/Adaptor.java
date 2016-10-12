package com.keendly.adaptors;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.FeedEntry;
import com.keendly.adaptors.model.auth.Credentials;
import com.keendly.adaptors.model.auth.Token;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WSClient;

import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected static final int MAX_ARTICLES_PER_FEED = 100;
    protected static final long TIMEOUT_IN_SECONDS = 30;

    protected abstract Promise<Token> doLogin(Credentials credentials);
    protected abstract Promise<ExternalUser> doGetUser();
    protected abstract Promise<List<ExternalFeed>> doGetFeeds();
    protected abstract Promise<Map<String, List<FeedEntry>>> doGetUnread(List<String> feedIds);
    protected abstract Promise<Map<String, Integer>> doGetUnreadCount(List<String> feedIds);
    protected abstract Promise<Boolean> doMarkAsRead(List<String> feedIds, long timestamp);
    protected abstract Promise<Boolean> doMarkArticleRead(List<String> articleIds);
    protected abstract Promise<Boolean> doMarkArticleUnread(List<String> articleIds);

    protected Token token;
    protected WSClient client;

    public Promise<Token> login(Credentials credentials){
        return this.doLogin(credentials).map(token -> {
            this.token = token;
            return token;
        });
    }

    public Promise<ExternalUser> getUser(){
        validateLoggedIn();
        return doGetUser();
    }

    public Promise<List<ExternalFeed>> getFeeds(){
        validateLoggedIn();
        return doGetFeeds();
    }

    public Promise<Map<String, List<FeedEntry>>> getUnread(List<String> feedIds){
        validateLoggedIn();
        return doGetUnread(feedIds);
    }

    public Promise<Map<String, Integer>> getUnreadCount(List<String> feedIds){
        validateLoggedIn();
        return doGetUnreadCount(feedIds);
    }

    public Promise<Boolean> markAsRead(List<String> feedIds, long timestamp){
        validateLoggedIn();
        return doMarkAsRead(feedIds, timestamp);
    }

    public Promise<Boolean> markArticleRead(List<String> articleIds){
        validateLoggedIn();
        return doMarkArticleRead(articleIds);
    }

    public Promise<Boolean> markArticleUnread(List<String> articleIds){
        validateLoggedIn();
        return doMarkArticleUnread(articleIds);
    }

    private void validateLoggedIn(){
        if (token == null){
            throw new IllegalStateException("Log in first");
        }
    }

    protected static boolean isOk(int status){
        return status == HttpStatus.SC_OK;
    }

    protected static boolean isUnauthorized(int status){
        if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN){
            return true;
        }
        return false;
    }

    protected static String asText(JsonNode node, String field){
        JsonNode j = node.get(field);
        if (j != null){
            return j.asText();
        }
        return null;
    }

    protected static Date asDate(JsonNode node, String field){
        JsonNode j = node.get(field);
        if (j != null){
            Date d = new Date();
            d.setTime(j.asLong());
            return d;
        }
        return null;
    }

    public Token getToken(){
        return token;
    }
}
