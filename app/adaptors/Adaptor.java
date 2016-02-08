package adaptors;

import adaptors.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import entities.Provider;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;

import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected static final int MAX_ARTICLES_PER_FEED = 100;
    protected static final long TIMEOUT_IN_SECONDS = 30;

    protected abstract Promise<Token> doLogin(Credentials credentials);
    protected abstract Promise<ExternalUser> doGetUser();
    protected abstract Promise<List<ExternalFeed>> doGetFeeds();
    protected abstract Promise<Map<String, List<Entry>>> doGetUnread(List<String> feedIds);
    protected abstract Promise<Map<String, Integer>> doGetUnreadCount(List<String> feedIds);
    protected abstract Promise doMarkAsRead(List<String> feedIds);

    public abstract Provider getProvider();

    protected Token token;
    protected boolean isLoggedIn = false;

    public Adaptor(){

    }

    public Adaptor(Token token){
        this.token = token;
        this.isLoggedIn = true;
    }

    public Promise<Token> login(Credentials credentials){
        return this.doLogin(credentials).map(token -> {
            this.isLoggedIn = true;
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

    public Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds){
        validateLoggedIn();
        return doGetUnread(feedIds);
    }

    public Promise<Map<String, Integer>> getUnreadCount(List<String> feedIds){
        validateLoggedIn();
        return doGetUnreadCount(feedIds);
    }



    public Promise<Boolean> markAsRead(List<String> feedIds){
        validateLoggedIn();
        return doMarkAsRead(feedIds);
    }


    private void validateLoggedIn(){
        if (!isLoggedIn){
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
}
