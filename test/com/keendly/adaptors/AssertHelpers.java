package com.keendly.adaptors;

import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy;
import com.keendly.adaptors.model.ExternalFeed;
import com.keendly.adaptors.model.FeedEntry;
import org.apache.http.message.BasicNameValuePair;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.junit.Assert.assertEquals;

public class AssertHelpers {

    public static ValueMatchingStrategy thatContainsParams(BasicNameValuePair... params){
        StringBuilder sb = new StringBuilder();
        for (BasicNameValuePair param : params){
            sb.append(".*");
            sb.append(param.getName());
            sb.append("=");
            sb.append(param.getValue());
            sb.append(".*");
        }
        return matching(sb.toString());
    }

    public static BasicNameValuePair param(String key, String value){
        return new BasicNameValuePair(key, value);
    }

    public static void assertEntryCorrect(FeedEntry entry, String title, String author, int published,
                                    String url, String content){
        assertEquals(title, entry.getTitle());
        assertEquals(author, entry.getAuthor());
        assertEquals(url, entry.getUrl());
        assertEquals(content, entry.getContent());
        assertEquals(published, entry.getPublished().getTime());
    }


    public static void assertFeedCorrect(ExternalFeed feed, String title, String id){
        assertEquals(title, feed.getTitle());
        assertEquals(id, feed.getFeedId());
    }
}
