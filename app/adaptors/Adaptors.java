package adaptors;

import adaptors.feedly.FeedlyAdaptor;
import adaptors.inoreader.InoreaderAdaptor;
import adaptors.oldreader.OldReaderAdaptor;
import entities.Provider;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class Adaptors {

    private static Map<Provider, Class<? extends Adaptor>> ADAPTORS = new HashMap<>();
    static {
        ADAPTORS.put(Provider.FEEDLY, FeedlyAdaptor.class);
        ADAPTORS.put(Provider.OLDREADER, OldReaderAdaptor.class);
        ADAPTORS.put(Provider.INOREADER, InoreaderAdaptor.class);
    }

    public static Adaptor getByProvider(Provider provider){
        for (Provider p : ADAPTORS.keySet()){
            if (p == provider){
                try {
                    return ADAPTORS.get(p).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        throw new NotImplementedException("adaptor for " + provider.name() + " not found");
    }
}
