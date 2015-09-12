package adaptors.feedly;

import adaptors.Adaptor;
import adaptors.auth.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import play.Play;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.concurrent.TimeUnit;

public class FeedlyAdaptor extends Adaptor {

    private static String feedlyUrl;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;

    static {
        init();
    }

    private static void init(){
        feedlyUrl = configValue("feedly.url");
        clientId = configValue("feedly.client_id");
        clientSecret = configValue("feedly.client_secret");
        redirectUri = configValue("feedly.redirect_uri");
    }

    private static String configValue(String key){
        return Play.application().configuration().getString(key);
    }

    public Tokens getTokens(String code){
        JsonNode json = Json.newObject()
                .put("grant_type", "authorization_code")
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("code", code)
                .put("redirect_uri", redirectUri);
        F.Promise<WSResponse> res = WS.url(feedlyUrl + "/auth/token")
                .post(json);

        WSResponse r = res.get(timeoutInSeconds, TimeUnit.SECONDS);
        if (r.getStatus() == 200){
            JsonNode node = r.asJson();
            String refreshToken = node.findValue("refresh_token").asText();
            String accessToken = node.findValue("access_token").asText();
            return new Tokens(refreshToken, accessToken);
        } else {
            throw new RuntimeException(r.asJson().asText());
        }
    }
}
