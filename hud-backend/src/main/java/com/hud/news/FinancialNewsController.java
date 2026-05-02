package com.hud.news;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class FinancialNewsController {
    private final PlaywrightScraperService scraperService;

    public FinancialNewsController(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @GetMapping
    public List<NewsArticle> getNews() {
        return scraperService.scrapeYahooFinance();
    }
}
