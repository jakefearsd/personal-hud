package com.hud.news;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsDiscoveryService {
    public List<String> discoverRecentEvents() {
        return List.of("https://finance.yahoo.com/news/example"); // Mock for now
    }
}
