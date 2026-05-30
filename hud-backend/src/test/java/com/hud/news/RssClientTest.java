package com.hud.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("unit")
class RssClientTest {

    private HttpClient mockHttpClient;
    private RssClient rssClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        rssClient = new RssClient(mockHttpClient);
    }

    @Test
    void getLinksFromRss_parsesValidLinks() throws Exception {
        String rssXml = "<?xml version=\"1.0\"?>" +
                "<rss><channel>" +
                "<item><link>http://example.com/article1</link></item>" +
                "<item><link>http://example.com/article2.xml</link></item>" + // should be ignored
                "<item><link>http://example.com/rss/feed</link></item>" + // should be ignored
                "<item><link>http://example.com/news</link></item>" + // should be ignored
                "<item><link>http://example.com/article3</link></item>" +
                "</channel></rss>";

        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(rssXml);
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<String> links = rssClient.getLinksFromRss("http://fake.rss", 5);
        
        assertEquals(2, links.size());
        assertTrue(links.contains("http://example.com/article1"));
        assertTrue(links.contains("http://example.com/article3"));
    }

    @Test
    void getLinksFromRss_returnsEmptyListOnError() throws Exception {
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Network error"));

        List<String> links = rssClient.getLinksFromRss("http://fake.rss", 5);
        
        assertTrue(links.isEmpty());
    }
}
