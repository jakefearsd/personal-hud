package com.hud.news;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for high-level scraping operations using Playwright and native HTTP.
 * Acts as a context for various ScraperStrategies.
 */
@Service
public class PlaywrightScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    @FunctionalInterface
    private interface BrowserTask<T> {
        T execute(Page page);
    }

    private <T> T executeInBrowser(BrowserTask<T> task) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT)
                    .setLocale("en-US");
            Page page = browser.newContext(contextOptions).newPage();
            T result = task.execute(page);
            browser.close();
            return result;
        } catch (Exception e) {
            System.err.println("[PLAYWRIGHT] Error in browser task: " + e.getMessage());
            return null;
        }
    }

    public List<NewsArticle> scrapeYahooFinance() {
        return executeInBrowser(new YahooFinanceScraperStrategy()::scrape);
    }

    public List<String> getIswLinks(int limit) {
        return executeInBrowser(new IswScraperStrategy(limit)::scrape);
    }

    public MacroMetric scrapeYahooMetric(String ticker, String label) {
        return executeInBrowser(new YahooMetricScraperStrategy(ticker, label)::scrape);
    }

    public Double scrapeFredYieldSpread() {
        return executeInBrowser(new FredYieldScraperStrategy()::scrape);
    }

    public String extractFullText(String url) {
        String targetUrl = url.replace("&amp;", "&");
        
        return executeInBrowser(page -> {
            page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(45000));
            handleConsent(page);

            if (page.url().contains("google.com/") || page.url().contains("yahoo.com/")) {
                try { page.waitForURL(u -> !u.contains("google.com") && !u.contains("yahoo.com"), new Page.WaitForURLOptions().setTimeout(10000)); } catch (Exception e) {}
            }

            page.waitForTimeout(3000);
            String html = page.content();
            
            Readability4J readability = new Readability4J(targetUrl, html);
            Article article = readability.parse();
            String content = article.getTextContent();
            
            if (content == null || content.trim().length() < 100) return "";

            // Surgical Truncation
            String cleaned = content.trim();
            String[] markers = {"Endnotes", "Citations", "Technical Notes", "Authors:", "Related Publications", "Click here to see ISW"};
            for (String marker : markers) {
                int idx = cleaned.indexOf(marker);
                if (idx > 1000) {
                    cleaned = cleaned.substring(0, idx);
                }
            }
            return cleaned.length() > 1000000 ? cleaned.substring(0, 1000000) : cleaned.trim();
        });
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
            System.err.println("[RSS] Failed: " + e.getMessage());
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
        } catch (Exception e) {}
    }
}
