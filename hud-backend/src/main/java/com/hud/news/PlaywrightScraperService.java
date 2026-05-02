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

@Service
public class PlaywrightScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final String[] YAHOO_CONSENT_BTNS = {"button:has-text('Accept all')", "button:has-text('Alle akzeptieren')", "button:has-text('I agree')", "button:has-text('Agree')", "button[name='agree']", "button[value='agree']"};
    private static final String YAHOO_SELECTOR = "h3 a, a.subtle-link, a.js-content-viewer";

    public List<NewsArticle> scrapeYahooFinance() {
        return scrapeGeneral("https://finance.yahoo.com/news/", YAHOO_CONSENT_BTNS, YAHOO_SELECTOR, "https://finance.yahoo.com", 15);
    }

    public List<String> getLinksFromRss(String rssUrl, int limit) {
        System.out.println("[RSS] Fetching links from: " + rssUrl);
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
            System.out.println("[RSS] Found " + links.size() + " links.");
            return links;
        } catch (Exception e) {
            System.err.println("[RSS] Failed: " + e.getMessage());
            return List.of();
        }
    }

    public List<String> getIswLinks(int limit) {
        // Deep discovery via the publications page
        String url = "https://www.understandingwar.org/publications";
        String iswSelector = "a[href*='offensive-campaign-assessment'], a[href*='iran-update'], a[href*='israel-hamas-war-update'], a[href*='conflict-update']";
        System.out.println("[ISW] Discovery started on: " + url);
        List<String> links = scrapeLinks(url, iswSelector, "https://www.understandingwar.org", 25);
        return links.stream()
                .filter(l -> !l.contains("/about") && !l.contains("/terms") && !l.contains("/privacy") && !l.endsWith("/publications"))
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public String extractFullText(String url) {
        String targetUrl = url.replace("&amp;", "&");
        System.out.println("[EXTRACT] Starting extraction for: " + targetUrl);
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("en-US");
            Page page = browser.newContext(contextOptions).newPage();
            
            page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(45000));
            handleConsent(page);

            if (page.url().contains("google.com/") || page.url().contains("yahoo.com/")) {
                try { page.waitForURL(u -> !u.contains("google.com") && !u.contains("yahoo.com"), new Page.WaitForURLOptions().setTimeout(10000)); } catch (Exception e) {}
            }

            page.waitForTimeout(3000);
            String html = page.content();
            browser.close();

            Readability4J readability = new Readability4J(targetUrl, html);
            Article article = readability.parse();
            String content = article.getTextContent();
            
            if (content == null || content.trim().length() < 100) return "";

            // Surgical Truncation for Narrative Resolution
            String cleaned = content.trim();
            String[] markers = {"Endnotes", "Citations", "Technical Notes", "Authors:", "Related Publications", "Click here to see ISW"};
            for (String marker : markers) {
                int idx = cleaned.indexOf(marker);
                if (idx > 1000) {
                    cleaned = cleaned.substring(0, idx);
                }
            }
            
            // Return high-signal block
            return cleaned.length() > 20000 ? cleaned.substring(0, 20000) : cleaned.trim();
        } catch (Exception e) {
            System.err.println("[EXTRACT] Failed: " + e.getMessage());
            return "";
        }
    }

    private List<String> scrapeLinks(String url, String selector, String baseUrl, int limit) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT)).newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            handleConsent(page);
            List<String> links = new ArrayList<>();
            page.locator(selector).all().forEach(l -> {
                try {
                    String href = l.getAttribute("href");
                    if (href != null && !href.isBlank()) {
                        String abs = href.startsWith("/") ? baseUrl + href : href;
                        if (!links.contains(abs)) links.add(abs);
                    }
                } catch (Exception e) {}
            });
            browser.close();
            return links.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    private List<NewsArticle> scrapeGeneral(String url, String[] consentBtns, String selector, String baseUrl, int limit) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT)).newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            handleConsent(page);
            page.waitForTimeout(2000);
            List<NewsArticle> articles = new ArrayList<>();
            page.locator(selector).all().forEach(l -> {
                try {
                    String title = l.innerText();
                    String href = l.getAttribute("href");
                    if (title != null && title.length() > 10 && href != null) {
                        articles.add(new NewsArticle(title.trim(), href.startsWith("/") ? baseUrl + href : href));
                    }
                } catch (Exception e) {}
            });
            browser.close();
            return articles.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
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
