package adaptors;

import adaptors.feedly.FeedlyAdaptor;
import adaptors.inoreader.InoreaderAdaptor;
import entities.Provider;

public class Adaptors {

    private static FeedlyAdaptor feedlyAdaptor = new FeedlyAdaptor();
    private static InoreaderAdaptor inoreaderAdaptor = new InoreaderAdaptor();

    public static Adaptor getByProvider(Provider provider){
        switch (provider){
            case FEEDLY:
                return feedlyAdaptor;
            case INOREADER:
                return inoreaderAdaptor;
            default:
                throw new IllegalArgumentException("not supported"); // TODO;
        }
    }
}
