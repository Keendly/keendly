package adaptors;

import adaptors.model.Entry;
import adaptors.model.SubscribedFeed;
import adaptors.model.Token;
import adaptors.model.ExternalUser;
import play.libs.F.Promise;

import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Token> login(String authorizationCode);
    public abstract Promise<ExternalUser> getUser(Token token);
    public abstract Promise<List<SubscribedFeed>> getSubscribedFeeds(Token token);
    public abstract Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Token token);
    public abstract Promise markAsRead(List<String> feedIds, Token token);
}
