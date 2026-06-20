package com.hud.news;

import com.hud.briefing.BriefingCategory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Facade for scraping operations. Delegated to specialized components for
 * browser management, RSS fetching, and content cleaning.
 */
@Service
public class PlaywrightScraperService {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightScraperService.class);
    private static final int MAX_VISITED_PAGES = 10;
    private static final int DEEP_CRAWL_BRANCH_LIMIT = 3;

    private final PlaywrightBrowserManager browserManager;
    private final ContentCleaner contentCleaner;
    private final RssClient rssClient;

    public PlaywrightScraperService(PlaywrightBrowserManager browserManager,
                                    ContentCleaner contentCleaner,
                                    RssClient rssClient) {
        this.browserManager = browserManager;
        this.contentCleaner = contentCleaner;
        this.rssClient = rssClient;
    }

    public List<NewsArticle> scrapeYahooFinance() {
        return browserManager.executeInBrowser(new YahooFinanceScraperStrategy()::scrape);
    }

    public List<String> getIswLinks(int limit, BriefingCategory category) {
        return browserManager.executeInBrowser(new IswScraperStrategy(limit, category)::scrape);
    }

    public List<String> getCsisLinks(int limit) {
        return browserManager.executeInBrowser(new CsisScraperStrategy(limit)::scrape);
    }

    public MacroMetric scrapeYahooMetric(String ticker, String label) {
        return browserManager.executeInBrowser(new YahooMetricScraperStrategy(ticker, label)::scrape);
    }

    public Double scrapeFredYieldSpread() {
        return browserManager.executeInBrowser(new FredYieldScraperStrategy()::scrape);
    }

    public List<String> getLinksFromRss(String rssUrl, int limit) {
        return rssClient.getLinksFromRss(rssUrl, limit);
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
        
        String content = browserManager.executeInBrowser(page -> {
            try {
                logger.debug("[PLAYWRIGHT] Navigating to: {}", targetUrl);
                page.navigate(targetUrl, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45000));
                
                handleConsent(page);
                handleRedirects(page);

                page.waitForTimeout(1000);
                String html = page.content();
                
                Readability4J readability = new Readability4J(targetUrl, html);
                Article article = readability.parse();
                String text = article.getTextContent();
                
                if (text == null || text.trim().length() < 100) {
                    logger.warn("[PLAYWRIGHT] Insufficient content extracted from {}", targetUrl);
                    return "";
                }

                String cleaned = contentCleaner.clean(text);
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
                page.waitForURL(u -> !u.contains("google.com") && !u.contains("yahoo.com"), 
                        new Page.WaitForURLOptions().setTimeout(10000)); 
            } catch (Exception e) {
                logger.debug("[PLAYWRIGHT] Timeout waiting for redirect from {}", page.url());
            }
        }
    }

    private void performDeepCrawl(String targetUrl, String htmlContent, int depth, Set<String> visited, StringBuilder sb) {
        if (htmlContent == null) return;
        
        Document doc = Jsoup.parse(htmlContent, targetUrl);
        List<Element> links = doc.select("a[href]");
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

    void handleConsent(Page page) {
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
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
