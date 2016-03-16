package com.keendly.adaptors;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.model.Entry;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.ExternalUser;
import com.keendly.adaptors.model.Token;
import play.libs.F;
import play.libs.ws.WSResponse;

import java.util.*;
import java.util.function.Function;

public abstract class GoogleReaderTypeAdaptor extends Adaptor {

    protected abstract <T> F.Promise<T> get(String url, Function<WSResponse, T> callback);
    protected abstract <T> F.Promise<T> getFlat(String url, Function<WSResponse, F.Promise<T>> callback);

    protected String normalizeFeedId(String feedId){
        return feedId;
    }

    public GoogleReaderTypeAdaptor(){

    }

    public GoogleReaderTypeAdaptor(Token token) {
        super(token);
    }


    @Override
    public F.Promise<Map<String, List<Entry>>> doGetUnread(List<String> feedIds) {
        return getUnreadCount(feedIds).flatMap(unreadCounts -> {
            List<F.Promise<Map<String, List<Entry>>>> resultsPromises = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unreadCounts.entrySet()){
                F.Promise<Map<String, List<Entry>>> entries = doGetUnread(entry.getKey(), entry.getValue(), null);
                resultsPromises.add(entries);
            }

            return F.Promise.sequence(resultsPromises).map(ret -> {
                Map<String, List<Entry>> entries = new HashMap<>();
                for (Map<String, List<Entry>> entry : ret) {
                    for (Map.Entry<String, List<Entry>> mapEntry : entry.entrySet()) {
                        entries.put(mapEntry.getKey(), mapEntry.getValue());
                    }
                }
                return entries;
            });
        });
    }

    private F.Promise<Map<String, List<Entry>>> doGetUnread(String feedId, int unreadCount, String continuation){
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url ="/stream/contents/" +normalizeFeedId(feedId) + "?xt=user/-/state/com.google/read";
        url = continuation == null ? url : url + "&c=" + continuation;
        return getFlat(url, response -> {
            JsonNode items = response.asJson().get("items");
            if (items == null){
                return F.Promise.pure(Collections.emptyMap());
            }
            List<Entry> entries = new ArrayList<>();
            for (JsonNode item : items){
                Entry entry = new Entry();
                entry.setUrl(extractArticleUrl(item));
                entry.setTitle(asText(item, "title"));
                entry.setAuthor(asText(item, "author"));
                entry.setPublished(asDate(item, "published"));
                entry.setContent(extractContent(item));
                entries.add(entry);
            }
            Map<String, List<Entry>> ret = new HashMap<>();
            ret.put(feedId, entries);
            if (ret.get(feedId).size() < count){
                JsonNode continuationNode = response.asJson().get("continuation");
                boolean hasContinuation = continuationNode != null;
                if (hasContinuation){
                    F.Promise<Map<String, List<Entry>>> nextPagePromise =
                            doGetUnread(feedId, count - ret.get(feedId).size(), continuationNode.asText());
                    return nextPagePromise.map(nextPage -> {
                        ret.get(feedId).addAll(nextPage.get(feedId));
                        return ret;
                    });
                }
            }

            return F.Promise.pure(ret);
        });
    }


    private String extractArticleUrl(JsonNode node){
        if (node.get("alternate") != null){
            for (JsonNode alternate : node.get("alternate")) {
                if (alternate.get("type").asText().equals("text/html")) {
                    return alternate.get("href").asText();
                }
            }
        } else if (node.get("canonical") != null) {
            for (JsonNode canonical : node.get("canonical")) {
                return canonical.get("href").asText();
            }
        }
        return null;
    }

    private String extractContent(JsonNode item){
        if (item.get("content") != null && item.get("content").get("content") != null){
            return item.get("content").get("content").asText();
        } else if (item.get("summary") != null && item.get("summary").get("content") != null){
            return item.get("summary").get("content").asText();
        }
        return null;
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
