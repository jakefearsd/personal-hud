package com.hud.news;

/**
 * Strategy for cleaning extracted text from various sources.
 */
public interface ContentCleaner {
    String clean(String text);
}
