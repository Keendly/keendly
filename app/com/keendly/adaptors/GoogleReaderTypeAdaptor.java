package com.keendly.adaptors;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.model.FeedEntry;
import org.apache.http.HttpStatus;
import play.libs.F;
import play.libs.ws.WSResponse;

import java.util.*;
import java.util.function.Function;

public abstract class GoogleReaderTypeAdaptor extends Adaptor {

    protected abstract <T> F.Promise<T> get(String url, Map<String, String> params,
                                            Function<WSResponse, T> callback);
    protected abstract <T> F.Promise<T> getFlat(String url, Map<String, String> params,
                                                Function<WSResponse, F.Promise<T>> callback);

    protected abstract <T> F.Promise<T> post(String url, Map<String, String> params,
                                            Function<WSResponse, T> callback);

    protected <T> F.Promise<T> get(String url, Function<WSResponse, T> callback){
        return get(url, Collections.emptyMap(), callback);
    }

    protected <T> F.Promise<T> getFlat(String url, Function<WSResponse, F.Promise<T>> callback){
        return getFlat(url, Collections.emptyMap(), callback);
    }

    protected String normalizeFeedId(String feedId){
        return feedId;
    }

    @Override
    protected F.Promise<Map<String, List<FeedEntry>>> doGetUnread(List<String> feedIds) {
        return getUnreadCount(feedIds).flatMap(unreadCounts -> {
            List<F.Promise<Map<String, List<FeedEntry>>>> resultsPromises = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unreadCounts.entrySet()){
                F.Promise<Map<String, List<FeedEntry>>> entries = doGetUnread(entry.getKey(), entry.getValue(), null);
                resultsPromises.add(entries);
            }

            return F.Promise.sequence(resultsPromises).map(ret -> {
                Map<String, List<FeedEntry>> entries = new HashMap<>();
                for (Map<String, List<FeedEntry>> entry : ret) {
                    for (Map.Entry<String, List<FeedEntry>> mapEntry : entry.entrySet()) {
                        entries.put(mapEntry.getKey(), mapEntry.getValue());
                    }
                }
                return entries;
            });
        });
    }

    private F.Promise<Map<String, List<FeedEntry>>> doGetUnread(String feedId, int unreadCount, String continuation){
        int count = unreadCount > MAX_ARTICLES_PER_FEED ? MAX_ARTICLES_PER_FEED : unreadCount; // TODO inform user
        String url ="/stream/contents/" +normalizeFeedId(feedId) + "?xt=user/-/state/com.google/read";
        url = continuation == null ? url : url + "&c=" + continuation;
        return getFlat(url, response -> {
            JsonNode jsonResponse = response.asJson();
            JsonNode items = jsonResponse.get("items");
            if (items == null){
                return F.Promise.pure(Collections.emptyMap());
            }
            List<FeedEntry> entries = new ArrayList<>();
            for (JsonNode item : items){
                FeedEntry entry = new FeedEntry();
                entry.setUrl(GoogleReaderMapper.extractArticleUrl(item));
                entry.setId(asText(item, "id"));
                entry.setTitle(asText(item, "title"));
                entry.setAuthor(asText(item, "author"));
                entry.setPublished(asDate(item, "published"));
                entry.setContent(GoogleReaderMapper.extractContent(item));
                entries.add(entry);
            }
            Map<String, List<FeedEntry>> ret = new HashMap<>();
            ret.put(feedId, entries);
            if (ret.get(feedId).size() < count){
                JsonNode continuationNode = jsonResponse.get("continuation");
                boolean hasContinuation = continuationNode != null;
                if (hasContinuation){
                    F.Promise<Map<String, List<FeedEntry>>> nextPagePromise =
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

    @Override
    protected F.Promise<Boolean> doMarkArticleRead(List<String> articleIds){
        return editTag(true, "user/-/state/com.google/read", articleIds);
    }

    @Override
    protected F.Promise<Boolean> doMarkArticleUnread(List<String> articleIds){
        return editTag(false, "user/-/state/com.google/read", articleIds);
    }

    private F.Promise<Boolean> editTag(boolean add, String tag, List<String> ids){
        Map<String, String> params = new HashMap<>();
        String action = add ? "a" : "r";
        params.put(action, tag);
        for (String id : ids){
            params.put("id", id);
        }

        return post("/edit-tag", params, response -> {
            if (response.getStatus() != HttpStatus.SC_OK){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        });

    }
}
