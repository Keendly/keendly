package adaptors;

import adaptors.feedly.FeedlyAdaptor;
import models.Provider;

public class Adaptors {

    private static FeedlyAdaptor feedlyAdaptor = new FeedlyAdaptor();

    public static Adaptor getByProvider(Provider provider){
        switch (provider){
            case FEEDLY:
                return feedlyAdaptor;
            default:
                throw new IllegalArgumentException("not supported"); // TODO;
        }
    }
}
