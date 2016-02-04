package adaptors.oldreader;

import adaptors.GoogleReaderTypeAdaptor;
import adaptors.exception.ApiException;
import adaptors.model.*;
import entities.Provider;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

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
    protected Promise<Map<String, List<Entry>>> doGetUnread(List<String> feedIds) {
        return null;
    }

    @Override
    protected Promise doMarkAsRead(List<String> feedIds) {
        return null;
    }

    private <T> Promise<T> get(String url, Function<WSResponse, T> callback){
        Promise<WSResponse> res =  WS.url(URL + url + "?output=json")
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
    public Provider getProvider() {
        return Provider.OLDREADER;
    }
}
