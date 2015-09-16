package adaptors.feedly;

import adaptors.Adaptor;
import adaptors.auth.Tokens;
import adaptors.auth.User;
import adaptors.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.function.Function;

import static utils.ConfigUtils.parameter;

public class FeedlyAdaptor extends Adaptor {

    enum FeedlyParam {
        URL("feedly.url"),
        CLIENT_ID("feedly.client_id"),
        CLIENT_SECRET("feedly.client_secret"),
        REDIRECT_URL("feedly.redirect_uri");

        String value;
        FeedlyParam(String value){
            this.value = value;
        }
    }

    private static String feedlyUrl;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;

    static {
        init();
    }

    private static void init(){
        feedlyUrl = parameter(FeedlyParam.URL.value);
        clientId = parameter(FeedlyParam.CLIENT_ID.value);
        clientSecret = parameter(FeedlyParam.CLIENT_SECRET.value);
        redirectUri = parameter(FeedlyParam.REDIRECT_URL.value);
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
                        String refreshToken = node.get("refresh_token").asText();
                        String accessToken = node.get("access_token").asText();
                        return new Tokens(refreshToken, accessToken);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    @Override
    public Promise<User> getUser(Tokens tokens){
        return doGet(feedlyUrl + "/profile", tokens, response -> {
            User user = new User();
            JsonNode node = response.asJson();
            user.setId(node.get("id").asText());
            user.setUserName(node.get("email").asText());
            return user;
        });
    }

    private <T> Promise<T> doGet(String url, Tokens tokens, Function<WSResponse, T> callback){
        Promise<WSResponse> res = WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get();
        return res
                .flatMap(response -> {
                    if (ok(response)) {
                        return Promise.pure(callback.apply(response));
                    } else if (unauthorized(response)) {
                        Promise token = refreshAccessToken(tokens.getRefreshToken());
                        return token.flatMap(newToken -> {
                            tokens.setAccessToken((String) newToken);
                            return doGetNoRefresh(url, tokens, callback);
                        });
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
    }

    private <T> Promise<T> doGetNoRefresh(String url, Tokens tokens, Function<WSResponse, T> callback){
        return WS.url(url)
                .setHeader("Authorization", "OAuth " + tokens.getAccessToken())
                .get()
                .map(response -> {
                    if (ok(response)) {
                        return callback.apply(response);
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
                    }
                });
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
                        return node.get("access_token").asText();
                    } else {
                        throw new ApiException(response.getStatus(), response.getBody());
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
