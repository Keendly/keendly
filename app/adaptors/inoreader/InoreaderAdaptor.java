package adaptors.inoreader;

import adaptors.GoogleReaderTypeAdaptor;
import adaptors.exception.ApiException;
import adaptors.model.*;
import entities.Provider;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InoreaderAdaptor extends GoogleReaderTypeAdaptor {

    private static final String URL = "https://www.inoreader.com/reader/api/0";
    private static final String APP_ID = "1000001083";
    private static final String APP_KEY = "LiFY_ZeWCm70HT62kN17wnQlki3BjJtX";

    public InoreaderAdaptor(){

    }

    public InoreaderAdaptor(Token token) {
        super(token);
    }

    @Override
    public Promise<Token> doLogin(Credentials credentials) {
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

    @Override
    public Promise<ExternalUser> doGetUser() {
        return get("/user-info", response -> toUser(response.asJson()));
    }

    @Override
    public Promise<List<ExternalFeed>> doGetFeeds() {
        return get("/subscription/list", response ->  toFeeds(response.asJson()));
    }

    private <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res =  WS.url(URL + url)
                .setHeader("AppId", APP_ID)
                .setHeader("AppKey", APP_KEY)
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
    public Promise<Map<String, List<Entry>>> doGetUnread(List<String> feedIds) {
        throw new NotImplementedException("bum");
    }

    @Override
    public Promise doMarkAsRead(List<String> feedIds) {
        throw new NotImplementedException("pim");
    }

    @Override
    public Provider getProvider() {
        return Provider.INOREADER;
    }
}