package adaptors;

import adaptors.model.Entry;
import adaptors.model.ExternalSubscription;
import adaptors.model.Tokens;
import adaptors.model.User;
import play.libs.F.Promise;

import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Tokens> getTokens(String code);
    public abstract Promise<User> getUser(Tokens tokens);
    public abstract Promise<List<ExternalSubscription>> getSubscriptions(Tokens tokens);
    public abstract Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Tokens tokens);
    public abstract Promise markAsRead(List<String> feedIds, Tokens tokens);
}
