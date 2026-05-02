package com.hud.news;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
import java.util.stream.Collectors;

/**
 * Service for high-level scraping operations using Playwright and native HTTP.
 */
@Service
public class PlaywrightScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final String[] YAHOO_CONSENT_BTNS = {"button:has-text('Accept all')", "button:has-text('Alle akzeptieren')", "button:has-text('I agree')", "button:has-text('Agree')", "button[name='agree']", "button[value='agree']"};
    private static final String YAHOO_SELECTOR = "h3 a, a.subtle-link, a.js-content-viewer";

    public List<NewsArticle> scrapeYahooFinance() {
        return scrapeGeneral("https://finance.yahoo.com/news/", YAHOO_CONSENT_BTNS, YAHOO_SELECTOR, "https://finance.yahoo.com", 15);
    }

    /**
     * Fetch article links from an RSS feed using native HTTP to avoid bot-detection on discovery.
     */
    public List<String> getLinksFromRss(String rssUrl, int limit) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(rssUrl))
                    .header("User-Agent", USER_AGENT)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            List<String> links = new ArrayList<>();
            // Extract links from <link> tags in RSS
            Pattern pattern = Pattern.compile("<link>(https?://[^<]+)</link>");
            Matcher matcher = pattern.matcher(body);
            while (matcher.find() && links.size() < limit) {
                String link = matcher.group(1);
                // Filter out feed links and XML files
                if (!link.endsWith(".xml") && !link.contains("/rss") && !link.endsWith("/news") && !links.contains(link)) {
                    links.add(link);
                }
            }
            return links;
        } catch (Exception e) {
            System.err.println("Failed to fetch RSS from " + rssUrl + ": " + e.getMessage());
            return List.of();
        }
    }

    public List<String> getIswLinks(int limit) {
        String url = "https://www.understandingwar.org/";
        // Target daily assessments and conflict updates
        String iswSelector = "a[href*='offensive-campaign-assessment'], a[href*='conflict-update'], a[href*='ukraine-conflict-updates']";
        List<String> links = scrapeLinks(url, iswSelector, "https://www.understandingwar.org", 15);
        
        return links.stream()
                .filter(l -> !l.contains("/about"))
                .filter(l -> !l.contains("/terms"))
                .filter(l -> !l.contains("/privacy"))
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public String extractFullText(String url) {
        // Clean the URL (RSS links often have &amp;)
        String targetUrl = url.replace("&amp;", "&");
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("en-US");
            Page page = browser.newContext(contextOptions).newPage();
            
            // Generous navigation timeout
            page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(60000));
            
            handleConsent(page);

            // Follow redirects away from known walled domains
            if (page.url().contains("google.com/") || page.url().contains("yahoo.com/")) {
                try {
                    page.waitForURL(u -> !u.contains("google.com") && !u.contains("yahoo.com"), new Page.WaitForURLOptions().setTimeout(10000));
                } catch (Exception e) {}
            }

            page.waitForTimeout(3000);
            String html = page.content();
            browser.close();

            Readability4J readability = new Readability4J(targetUrl, html);
            Article article = readability.parse();
            String content = article.getTextContent();
            return (content != null) ? content.trim() : "";
        } catch (Exception e) {
            System.err.println("Failed to extract text from " + url + ": " + e.getMessage());
            return "";
        }
    }

    private List<String> scrapeLinks(String url, String selector, String baseUrl, int limit) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("en-US");
            Page page = browser.newContext(contextOptions).newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            handleConsent(page);
            List<String> links = new ArrayList<>();
            page.locator(selector).all().forEach(locator -> {
                try {
                    String href = locator.getAttribute("href");
                    if (href != null && !href.isBlank()) {
                        String absUrl = href.startsWith("/") ? baseUrl + href : href;
                        if (!links.contains(absUrl)) links.add(absUrl);
                    }
                } catch (Exception e) {}
            });
            browser.close();
            return links;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<NewsArticle> scrapeGeneral(String url, String[] consentBtns, String selector, String baseUrl, int limit) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("en-US");
            Page page = browser.newContext(contextOptions).newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            handleConsent(page);
            page.waitForTimeout(3000);
            List<NewsArticle> articles = new ArrayList<>();
            page.locator(selector).all().forEach(locator -> {
                try {
                    String title = locator.innerText();
                    String href = locator.getAttribute("href");
                    if (title != null && title.trim().length() > 10 && href != null && !href.isBlank()) {
                        String absUrl = href.startsWith("/") ? baseUrl + href : href;
                        articles.add(new NewsArticle(title.trim().replaceAll("\\s+", " "), absUrl));
                    }
                } catch (Exception e) {}
            });
            browser.close();
            return articles.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private void handleConsent(Page page) {
        try {
            String[] selectors = {"button:has-text('Accept all')", "button:has-text('Alle akzeptieren')", "button:has-text('I agree')", "button:has-text('Agree')", "button[name='agree']", "button[value='agree']"};
            for (String s : selectors) {
                var btn = page.locator(s).first();
                if (btn.isVisible()) {
                    btn.click();
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    page.waitForTimeout(1000);
                    break;
                }
            }
        } catch (Exception e) {}
    }
}
