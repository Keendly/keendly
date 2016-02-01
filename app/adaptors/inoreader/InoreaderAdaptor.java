package adaptors.inoreader;

import adaptors.Adaptor;
import adaptors.exception.ApiException;
import adaptors.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InoreaderAdaptor extends Adaptor {

    private static final String URL = "https://www.inoreader.com/reader/api/0";
    private static final String APP_ID = "1000001083";
    private static final String APP_KEY = "LiFY_ZeWCm70HT62kN17wnQlki3BjJtX";

    @Override
    public Promise<Token> login(Credentials credentials) {
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
    public Promise<ExternalUser> getUser(Token token) {
        return WS.url(URL + "/user-info")
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .get()
                .map(response -> {
                    if (isOk(response.getStatus())) {
                        ExternalUser user = new ExternalUser();
                        JsonNode node = response.asJson();
                        user.setId(node.get("userId").asText());
                        user.setUserName(node.get("userEmail").asText());
                        user.setDisplayName(node.get("userName").asText());
                        return user;
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    public Promise<List<SubscribedFeed>> getSubscribedFeeds(Token token) {
        return WS.url(URL + "/subscription/list")
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
                .setHeader("Authorization", "GoogleLogin auth=" + token.getAccessToken())
                .get()
                .map(response -> {
                   if (isOk(response.getStatus())){
                       JsonNode json = response.asJson();
                       JsonNode subs = json.get("subscriptions");
                       List<SubscribedFeed> externalSubscriptions = new ArrayList<>();
                       for (JsonNode sub : subs){
                           SubscribedFeed externalSubscription = new SubscribedFeed();
                           externalSubscription.setFeedId(sub.get("id").asText());
                           externalSubscription.setTitle(sub.get("title").asText());
                           externalSubscriptions.add(externalSubscription);
                       }
                       return externalSubscriptions;
                    } else {
                       throw new ApiException(response.getStatus(), response.getBody());
                   }
                });
    }

    @Override
    public Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Token token) {
        return null;
    }

    @Override
    public Promise markAsRead(List<String> feedIds, Token token) {
        return null;
    }
}
