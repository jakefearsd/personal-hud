package com.hud.news;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Composite/Chain of Responsibility pattern for Content Cleaning.
 * Chains multiple cleaning strategies together.
 */
@Component
@Primary
public class CompositeContentCleaner implements ContentCleaner {
    
    private final List<ContentCleaner> cleaners;

    public CompositeContentCleaner() {
        this.cleaners = List.of(
            new WhitespaceContentCleaner(),
            new MarkerRemovalCleaner()
        );
    }

    @Override
    public String clean(String text) {
        String result = text;
        for (ContentCleaner cleaner : cleaners) {
            if (result == null || result.isBlank()) break;
            result = cleaner.clean(result);
        }
        return result;
    }
}
