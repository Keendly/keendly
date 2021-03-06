package com.keendly.adaptors;

import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy;
import com.google.common.net.UrlEscapers;
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
            sb.append(UrlEscapers.urlFormParameterEscaper().escape(param.getName()));
            sb.append("=");
            sb.append(UrlEscapers.urlFormParameterEscaper().escape(param.getValue()));
            sb.append(".*");
        }
        return matching(sb.toString());
    }

    public static BasicNameValuePair param(String key, String value){
        return new BasicNameValuePair(key, value);
    }

    public static void assertEntryCorrect(FeedEntry entry, String id, String title, String author, long published,
                                    String url, String content){
        assertEquals(id , entry.getId());
        assertEquals(title, entry.getTitle());
        assertEquals(author, entry.getAuthor());
        assertEquals(url, entry.getUrl());
        assertEquals(content, entry.getContent());
        assertEquals(published * 1000, entry.getPublished().getTime());
    }


    public static void assertFeedCorrect(ExternalFeed feed, String title, String id){
        assertEquals(title, feed.getTitle());
        assertEquals(id, feed.getFeedId());
    }
}
