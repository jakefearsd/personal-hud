package com.hud.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for fetching and parsing RSS feeds.
 */
@Component
public class RssClient {

    private static final Logger logger = LoggerFactory.getLogger(RssClient.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    private final HttpClient httpClient;

    public RssClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public List<String> getLinksFromRss(String rssUrl, int limit) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(rssUrl))
                    .header("User-Agent", USER_AGENT)
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            List<String> links = new ArrayList<>();
            Pattern pattern = Pattern.compile("<link>(https?://[^<]+)</link>");
            Matcher matcher = pattern.matcher(body);
            while (matcher.find() && links.size() < limit) {
                String link = matcher.group(1);
                if (!link.endsWith(".xml") && !link.contains("/rss") && !link.endsWith("/news") && !links.contains(link)) {
                    links.add(link);
                }
            }
            return links;
        } catch (Exception e) {
            logger.error("[RSS] Failed to fetch links from {}: {}", rssUrl, e.getMessage());
            return List.of();
        }
    }
}
