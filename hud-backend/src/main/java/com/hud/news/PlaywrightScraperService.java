package com.hud.news;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for high-level scraping operations using Playwright and native HTTP.
 * Acts as a context for various ScraperStrategies.
 */
@Service
public class PlaywrightScraperService {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightScraperService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final int MAX_VISITED_PAGES = 10;
    private static final int DEEP_CRAWL_BRANCH_LIMIT = 3;

    @FunctionalInterface
    private interface BrowserTask<T> {
        T execute(Page page);
    }

    private <T> T executeInBrowser(BrowserTask<T> task) {
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setUserAgent(USER_AGENT)
                        .setLocale("en-US");
                try (BrowserContext context = browser.newContext(contextOptions)) {
                    Page page = context.newPage();
                    return task.execute(page);
                }
            }
        } catch (Exception e) {
            logger.error("[PLAYWRIGHT] Error in browser task", e);
            return null;
        }
    }

    public List<NewsArticle> scrapeYahooFinance() {
        return executeInBrowser(new YahooFinanceScraperStrategy()::scrape);
    }

    public List<String> getIswLinks(int limit) {
        return executeInBrowser(new IswScraperStrategy(limit)::scrape);
    }

    public List<String> getCsisLinks(int limit) {
        return executeInBrowser(new CsisScraperStrategy(limit)::scrape);
    }

    public MacroMetric scrapeYahooMetric(String ticker, String label) {
        return executeInBrowser(new YahooMetricScraperStrategy(ticker, label)::scrape);
    }

    public Double scrapeFredYieldSpread() {
        return executeInBrowser(new FredYieldScraperStrategy()::scrape);
    }

    public String extractFullText(String url) {
        return extractFullText(url, 0);
    }

    public String extractFullText(String url, int depth) {
        return extractFullTextInternal(url, depth, new HashSet<>());
    }

    private String extractFullTextInternal(String url, int depth, Set<String> visited) {
        if (visited.contains(url) || visited.size() > MAX_VISITED_PAGES) return "";
        visited.add(url);

        String targetUrl = url.replace("&amp;", "&");
        
        String content = executeInBrowser(page -> {
            try {
                page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(45000));
                handleConsent(page);
                handleRedirects(page);

                page.waitForTimeout(2000);
                String html = page.content();
                
                Readability4J readability = new Readability4J(targetUrl, html);
                Article article = readability.parse();
                String text = article.getTextContent();
                
                if (text == null || text.trim().length() < 100) {
                    logger.warn("[PLAYWRIGHT] Insufficient content extracted from {}", targetUrl);
                    return "";
                }

                String cleaned = cleanExtractedText(text);
                StringBuilder sb = new StringBuilder(cleaned);
                
                if (depth > 0) {
                    performDeepCrawl(targetUrl, article.getContent(), depth, visited, sb);
                }

                return sb.toString();
            } catch (Exception e) {
                logger.error("[PLAYWRIGHT] Failed to scrape {}: {}", targetUrl, e.getMessage());
                return "";
            }
        });

        return content != null ? truncateContent(content) : "";
    }

    private void handleRedirects(Page page) {
        if (page.url().contains("google.com/") || page.url().contains("yahoo.com/")) {
            try { 
                page.waitForURL(u -> !u.contains("google.com") && !u.contains("yahoo.com"), new Page.WaitForURLOptions().setTimeout(10000)); 
            } catch (Exception e) {
                logger.debug("[PLAYWRIGHT] Timeout waiting for redirect from {}", page.url());
            }
        }
    }

    String cleanExtractedText(String text) {
        String cleaned = text.trim();
        String[] markers = {"Endnotes", "Citations", "Technical Notes", "Authors:", "Related Publications", "Click here to see ISW"};
        for (String marker : markers) {
            int idx = cleaned.indexOf(marker);
            if (idx > 1000) {
                cleaned = cleaned.substring(0, idx);
            }
        }
        return cleaned;
    }

    private void performDeepCrawl(String targetUrl, String htmlContent, int depth, Set<String> visited, StringBuilder sb) {
        if (htmlContent == null) return;
        
        Document doc = Jsoup.parse(htmlContent, targetUrl);
        Elements links = doc.select("a[href]");
        int crawledCount = 0;
        URI baseUri = URI.create(targetUrl);

        for (Element link : links) {
            if (crawledCount >= DEEP_CRAWL_BRANCH_LIMIT) break;
            
            String href = link.attr("abs:href");
            if (isValidForDeepCrawl(href, baseUri.getHost())) {
                logger.info("[CRAWL] Deep diving into: {}", href);
                String deepText = extractFullTextInternal(href, depth - 1, visited);
                if (deepText.length() > 500) {
                    sb.append("\n\n--- RELATED INTEL: ").append(href).append(" ---\n").append(deepText);
                    crawledCount++;
                }
            }
        }
    }

    String truncateContent(String content) {
        return content.length() > 2000000 ? content.substring(0, 2000000) : content.trim();
    }

    boolean isValidForDeepCrawl(String href, String originalHost) {
        if (href == null || href.isBlank() || href.contains("#")) return false;
        try {
            URI uri = URI.create(href);
            String host = uri.getHost();
            if (host == null || !host.equals(originalHost)) return false;
            
            String path = uri.getPath().toLowerCase(Locale.ROOT);
            return !path.contains("/about") && !path.contains("/contact") && 
                   !path.contains("/terms") && !path.contains("/privacy") && 
                   !path.contains("/search") && !path.endsWith(".pdf");
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getLinksFromRss(String rssUrl, int limit) {
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(rssUrl)).header("User-Agent", USER_AGENT).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

    private void handleConsent(Page page) {
        try {
            String[] sels = {"button:has-text('Accept all')", "button:has-text('Agree')", "button[name='agree']"};
            for (String s : sels) {
                var btn = page.locator(s).first();
                if (btn.isVisible()) {
                    btn.click();
                    page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                    break;
                }
            }
        } catch (Exception e) {
            logger.debug("[PLAYWRIGHT] Could not handle consent dialog for {}: {}", page.url(), e.getMessage());
        }
    }
}
