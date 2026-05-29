package com.hud.news;

import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Manages the Playwright and Browser lifecycle.
 * Uses ThreadLocal to ensure thread-safety during concurrent scraping.
 */
@Component
public class PlaywrightBrowserManager {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserManager.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    private final ThreadLocal<Playwright> playwrightThreadLocal = ThreadLocal.withInitial(Playwright::create);
    
    private final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>() {
        @Override
        protected Browser initialValue() {
            logger.info("[PLAYWRIGHT] Launching browser for thread: {}", Thread.currentThread().getName());
            return playwrightThreadLocal.get().chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        }
    };

    @FunctionalInterface
    public interface BrowserTask<T> {
        T execute(Page page);
    }

    public <T> T executeInBrowser(BrowserTask<T> task) {
        Browser browser = browserThreadLocal.get();
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setLocale("en-US");

        try (BrowserContext context = browser.newContext(contextOptions);
             Page page = context.newPage()) {
            return task.execute(page);
        } catch (Exception e) {
            logger.error("[PLAYWRIGHT] Error in browser task", e);
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("[PLAYWRIGHT] Shutting down browser manager...");
        // ThreadLocal cleanup is tricky at shutdown, but for short-lived CLI tasks or standard Spring shutdown it's okay.
        // In a long-running app, we'd want to explicitly close these when threads die.
    }
}
