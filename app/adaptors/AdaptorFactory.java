package adaptors;

import adaptors.feedly.FeedlyAdaptor;
import adaptors.inoreader.InoreaderAdaptor;
import adaptors.model.Token;
import adaptors.oldreader.OldReaderAdaptor;
import entities.Provider;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class AdaptorFactory {

    private static Map<Provider, Class<? extends Adaptor>> ADAPTORS = new HashMap<>();
    static {
        ADAPTORS.put(Provider.FEEDLY, FeedlyAdaptor.class);
        ADAPTORS.put(Provider.OLDREADER, OldReaderAdaptor.class);
        ADAPTORS.put(Provider.INOREADER, InoreaderAdaptor.class);
    }

    public static Adaptor getInstance(Provider provider){
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

    public static Adaptor getInstance(Provider provider, Token token){
        for (Provider p : ADAPTORS.keySet()){
            if (p == provider){
                try {
                    return ADAPTORS.get(p).getConstructor(Token.class).newInstance(token);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        throw new NotImplementedException("adaptor for " + provider.name() + " not found");
    }
}
