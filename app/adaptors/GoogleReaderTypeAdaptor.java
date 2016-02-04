package adaptors;

import adaptors.model.ExternalFeed;
import adaptors.model.ExternalUser;
import adaptors.model.Token;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public abstract class GoogleReaderTypeAdaptor extends Adaptor {

    public GoogleReaderTypeAdaptor(){

    }

    public GoogleReaderTypeAdaptor(Token token) {
        super(token);
    }

    protected String extractToken(byte[] response){
        String s = new String(response);
        String[] params = s.split("\n");
        for (String param : params){
            String[] p = param.split("=");
            if (p.length == 2 && p[0].equals("Auth")){
                return p[1];
            }
        }
        return null;
    }

    protected ExternalUser toUser(JsonNode node){
        ExternalUser user = new ExternalUser();
        user.setId(node.get("userId").asText());
        user.setUserName(node.get("userEmail").asText());
        user.setDisplayName(node.get("userName").asText());
        return user;
    }

    protected List<ExternalFeed> toFeeds(JsonNode node){
        JsonNode subs = node.get("subscriptions");
        List<ExternalFeed> externalSubscriptions = new ArrayList<>();
        for (JsonNode sub : subs){
            ExternalFeed externalSubscription = new ExternalFeed();
            externalSubscription.setFeedId(sub.get("id").asText());
            externalSubscription.setTitle(sub.get("title").asText());
            externalSubscriptions.add(externalSubscription);
        }
        return externalSubscriptions;
    }

}
