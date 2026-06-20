package com.hud.news;

/**
 * Concrete strategy for content cleaning.
 * Normalizes excessive whitespace and newlines.
 */
public class WhitespaceContentCleaner implements ContentCleaner {
    @Override
    public String clean(String text) {
        if (text == null) return "";
        // Replace 3 or more newlines with just 2 newlines
        return text.replaceAll("(?m)^[ \t]*\r?\n{2,}", "\n\n").trim();
    }
}
