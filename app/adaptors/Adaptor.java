package adaptors;

import adaptors.model.Entry;
import adaptors.model.SubscribedFeed;
import auth.Tokens;
import adaptors.model.User;
import play.libs.F.Promise;

import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Tokens> login(String authorizationCode);
    public abstract Promise<User> getUser(Tokens tokens);
    public abstract Promise<List<SubscribedFeed>> getSubscribedFeeds(Tokens tokens);
    public abstract Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Tokens tokens);
    public abstract Promise markAsRead(List<String> feedIds, Tokens tokens);
}
