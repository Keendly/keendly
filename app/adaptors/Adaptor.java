package adaptors;

import adaptors.auth.Tokens;
import adaptors.auth.User;
import play.libs.F.Promise;

public abstract class Adaptor {

    protected long timeoutInSeconds = 30;

    public abstract Promise<Tokens> getTokens(String code);
    public abstract Promise<User> getUser(Tokens tokens);
}
