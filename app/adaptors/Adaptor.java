package adaptors;

import adaptors.model.*;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;

import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Token> login(Credentials credentials);
    public abstract Promise<ExternalUser> getUser(Token token);
    public abstract Promise<List<SubscribedFeed>> getSubscribedFeeds(Token token);
    public abstract Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Token token);
    public abstract Promise markAsRead(List<String> feedIds, Token token);

    protected boolean isOk(int status){
        return status == HttpStatus.SC_OK;
    }

    protected boolean isUnauthorized(int status){
        if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN){
            return true;
        }
        return false;
    }
}
