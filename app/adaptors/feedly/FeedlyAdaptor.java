package adaptors.feedly;

import adaptors.Adaptor;
import adaptors.auth.Tokens;
import adaptors.auth.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.function.Function;

import static utils.PlayUtils.configParam;

public class FeedlyAdaptor extends Adaptor {

    private static String feedlyUrl;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;

    static {
        init();
    }

    private static void init(){
        feedlyUrl = configParam("feedly.url");
        clientId = configParam("feedly.client_id");
        clientSecret = configParam("feedly.client_secret");
        redirectUri = configParam("feedly.redirect_uri");
    }

    @Override
    public Promise<Tokens> getTokens(String code){
        JsonNode json = Json.newObject()
                .put("grant_type", "authorization_code")
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("code", code)
                .put("redirect_uri", redirectUri);

        return WS.url(feedlyUrl + "/auth/token")
                .post(json)
                .map(response -> {
                    if (ok(response)) {
                        JsonNode node = response.asJson();
                        String refreshToken = node.findValue("refresh_token").asText();
                        String accessToken = node.findValue("access_token").asText();
                        return new Tokens(refreshToken, accessToken);
                    } else {
                        // todo log to sentry
                        throw new RuntimeException(response.asJson().asText());
                    }
                });
    }

    @Override
    public Promise<User> getUser(Tokens tokens){
        return doGet(feedlyUrl + "/profile", tokens, response -> {
            User user = new User();
            JsonNode node = response.asJson();
            user.setUserName(node.findValue("id").asText());
            user.setUserName(node.findValue("email").asText());
            return user;
        });
    }

    private <T> Promise<T> doGet(String url, Tokens tokens, Function<WSResponse, T> callback){
        Promise<WSResponse> res = WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get();
        return (Promise<T>) res
                .map(response -> {
                    if (ok(response)) {
                        return callback.apply(response);
                    } else if (unauthorized(response)) {
                        Promise token = refreshAccessToken(tokens.getRefreshToken());
                        return token.map(newToken -> {
                            tokens.setAccessToken((String) newToken);
                            return doGetNoRefresh(url, tokens, callback);
                        });
                    } else {
                        // todo log to sentry, handle in generic way
                        throw new RuntimeException(response.getBody());
                    }
                });
    }

    private Promise<?> doGetNoRefresh(String url, Tokens tokens, Function callback){
        return WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get()
                .map(response -> callback.apply(response));
    }

    private Promise<String> refreshAccessToken(String refreshToken){
        JsonNode json = Json.newObject()
                .put("grant_type", "refresh_token")
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("refresh_token", refreshToken);

        return WS.url(feedlyUrl + "/auth/token")
                .post(json)
                .map(response -> {
                    if (ok(response)) {
                        JsonNode node = response.asJson();
                        return node.findValue("access_token").asText();
                    } else {
                        // todo log to sentry, handle in generic way
                        throw new RuntimeException(response.asJson().asText());
                    }
                });
    }

    private boolean ok(WSResponse response){
        return response.getStatus() == HttpStatus.SC_OK;
    }

    private boolean unauthorized(WSResponse response){
        if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED ||
                response.getStatus() == HttpStatus.SC_FORBIDDEN){
            return true;
        }
        return false;
    }
}
