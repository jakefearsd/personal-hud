package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class YahooFinanceScraperStrategy implements ScraperStrategy<List<NewsArticle>> {
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceScraperStrategy.class);
    private static final String URL = "https://finance.yahoo.com/news/";
    private static final String SELECTOR = "h3 a, a.subtle-link, a.js-content-viewer";
    private static final String BASE_URL = "https://finance.yahoo.com";
    private static final int LIMIT = 15;

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public List<NewsArticle> scrape(Page page) {
        page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        handleConsent(page);
        page.waitForTimeout(2000);
        
        List<NewsArticle> articles = new ArrayList<>();
        page.locator(SELECTOR).all().forEach(l -> {
            try {
                String title = l.innerText();
                String href = l.getAttribute("href");
                if (title != null && title.length() > 10 && href != null) {
                    articles.add(new NewsArticle(title.trim(), href.startsWith("/") ? BASE_URL + href : href));
                }
            } catch (Exception e) {
                logger.warn("[MARKET] Failed to extract Yahoo News article: {}", e.getMessage());
            }
        });
        return articles.stream().limit(LIMIT).collect(Collectors.toList());
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
            logger.debug("[MARKET] Could not handle Yahoo consent dialog: {}", e.getMessage());
        }
    }
}
