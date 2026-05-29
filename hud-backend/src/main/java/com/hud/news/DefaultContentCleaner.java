package com.hud.news;

import org.springframework.stereotype.Component;

/**
 * Default implementation of content cleaning.
 * Removes common footers and metadata markers.
 */
@Component
public class DefaultContentCleaner implements ContentCleaner {

    private static final String[] MARKERS = {
        "Endnotes", "Citations", "Technical Notes", "Authors:", 
        "Related Publications", "Click here to see ISW"
    };

    @Override
    public String clean(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        for (String marker : MARKERS) {
            int idx = cleaned.indexOf(marker);
            if (idx > 1000) {
                cleaned = cleaned.substring(0, idx);
            }
        }
        return cleaned;
    }
}
