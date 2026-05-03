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
import java.util.stream.Collectors;

/**
 * Service for high-level scraping operations using Playwright and native HTTP.
 */
@Service
public class PlaywrightScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final String[] YAHOO_CONSENT_BTNS = {"button:has-text('Accept all')", "button:has-text('Alle akzeptieren')", "button:has-text('I agree')", "button:has-text('Agree')", "button[name='agree']", "button[value='agree']"};
    private static final String YAHOO_SELECTOR = "h3 a, a.subtle-link, a.js-content-viewer";

    @FunctionalInterface
    private interface BrowserTask<T> {
        T execute(Page page);
    }

    /**
     * Helper to manage Playwright browser/page lifecycle.
     */
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
        return scrapeGeneral("https://finance.yahoo.com/news/", YAHOO_CONSENT_BTNS, YAHOO_SELECTOR, "https://finance.yahoo.com", 15);
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

    public List<String> getIswLinks(int limit) {
        String url = "https://www.understandingwar.org/publications";
        String selector = "a[href*='offensive-campaign-assessment'], a[href*='iran-update'], a[href*='israel-hamas-war-update'], a[href*='conflict-update']";
        
        return executeInBrowser(page -> {
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            handleConsent(page);
            
            List<String> links = new ArrayList<>();
            page.locator(selector).all().forEach(l -> {
                try {
                    String href = l.getAttribute("href");
                    if (href != null && !href.isBlank()) {
                        String abs = href.startsWith("/") ? "https://www.understandingwar.org" + href : href;
                        if (!links.contains(abs)) links.add(abs);
                    }
                } catch (Exception e) {}
            });
            return links.stream()
                    .filter(l -> !l.contains("/about") && !l.contains("/terms") && !l.contains("/privacy") && !l.endsWith("/publications"))
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        });
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

    public MacroMetric scrapeYahooMetric(String ticker, String label) {
        String url = "https://finance.yahoo.com/quote/" + ticker;
        
        return executeInBrowser(page -> {
            try {
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(30000));
                handleConsent(page);
                
                // High-resolution stabilization wait
                page.waitForTimeout(8000);
                
                // Strictly symbol-locked selectors to prevent sidebar leakage
                Locator priceLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketPrice']").first();
                Locator changeLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChange']").first();
                Locator pctLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChangePercent']").first();

                if (priceLocator.count() == 0) {
                    throw new RuntimeException("Ticker " + ticker + " not found on page.");
                }

                String priceStr = priceLocator.innerText().trim();
                String changeStr = changeLocator.innerText().trim();
                String pctStr = pctLocator.innerText().trim();
                
                double price = Double.parseDouble(priceStr.replace(",", ""));
                double change = Double.parseDouble(changeStr.replace(",", ""));
                double pct = Double.parseDouble(pctStr.replace("(", "").replace(")", "").replace("%", ""));
                
                return new MacroMetric(ticker, label, price, change, pct);
            } catch (Exception e) {
                System.err.println("[MARKET] Failed to scrape " + ticker + ": " + e.getMessage());
                return null;
            }
        });
    }

    public Double scrapeFredYieldSpread() {
        String url = "https://fred.stlouisfed.org/series/T10Y2Y";
        return executeInBrowser(page -> {
            try {
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                String value = page.locator(".series-meta-observation-value").innerText();
                return Double.parseDouble(value.trim());
            } catch (Exception e) {
                System.err.println("[FRED] Failed to scrape yield spread: " + e.getMessage());
                return null;
            }
        });
    }

    private List<NewsArticle> scrapeGeneral(String url, String[] consentBtns, String selector, String baseUrl, int limit) {
        return executeInBrowser(page -> {
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
            return articles.stream().limit(limit).collect(Collectors.toList());
        });
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
