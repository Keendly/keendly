package adaptors;

import adaptors.auth.Entry;
import adaptors.auth.Subscription;
import adaptors.auth.Tokens;
import adaptors.auth.User;
import play.libs.F.Promise;

import java.util.List;
import java.util.Map;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Tokens> getTokens(String code);
    public abstract Promise<User> getUser(Tokens tokens);
    public abstract Promise<List<Subscription>> getSubscriptions(Tokens tokens);
    public abstract Promise<Map<String, List<Entry>>> getUnread(List<String> feedIds, Tokens tokens);
}
