package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownServiceTest {

    private MarkdownService markdownService;

    @BeforeEach
    void setUp() {
        markdownService = new MarkdownService();
    }

    @Test
    void shouldRenderBasicMarkdown() {
        String markdown = "# Title\n**bold** and *italic*";
        String html = markdownService.renderToHtml(markdown);

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<strong>bold</strong>"));
        assertTrue(html.contains("<em>italic</em>"));
    }

    @Test
    void shouldRenderLists() {
        String markdown = "* Item 1\n* Item 2\n\n1. First\n2. Second";
        String html = markdownService.renderToHtml(markdown);

        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<li>Item 1</li>"));
        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<li>First</li>"));
    }

    @Test
    void shouldRenderTacticalLinks() {
        String markdown = "Check out https://google.com for info.";
        String html = markdownService.renderToHtml(markdown);

        assertTrue(html.contains("<a href=\"https://google.com\" target=\"_blank\" rel=\"noopener noreferrer\" class=\"tactical-link\">https://google.com</a>"));
    }

    @Test
    void shouldRenderTacticalTables() {
        String markdown = "| Head | Value |\n| --- | --- |\n| Stat | 100 |";
        String html = markdownService.renderToHtml(markdown);

        assertTrue(html.contains("<table class=\"tactical-table\">"));
        assertTrue(html.contains("<thead>"));
        assertTrue(html.contains("<th>Head</th>"));
        assertTrue(html.contains("<td>Stat</td>"));
    }

    @Test
    void shouldRenderStrikethrough() {
        String markdown = "This is ~~wrong~~ right.";
        String html = markdownService.renderToHtml(markdown);

        assertTrue(html.contains("<del>wrong</del>"));
    }

    @Test
    void shouldSanitizeDangerousHtml() {
        String markdown = "Dangerous <script>alert('xss')</script> content <img src=x onerror=alert(1)>";
        String html = markdownService.renderToHtml(markdown);

        assertFalse(html.contains("<script>"), "Should not contain script tag");
        assertFalse(html.contains("onerror"), "Should not contain onerror attribute");
        // Jsoup might normalize whitespaces when removing tags
        assertTrue(html.contains("Dangerous") && html.contains("content"), "Should contain safe text content");
    }

    @Test
    void shouldHandleNullOrBlank() {
        assertEquals("", markdownService.renderToHtml(null));
        assertEquals("", markdownService.renderToHtml("   "));
    }

    @Test
    void shouldNormalizeSquashedTables() {
        // This is a "messy" table where the header and separator are on the same line,
        // and it follows text without a blank line.
        String messyMarkdown = "Text before table\n| Target | Location | Distance | | :--- | :--- | :--- |\n| R1 | L1 | D1 |";
        String html = markdownService.renderToHtml(messyMarkdown);

        assertTrue(html.contains("<table class=\"tactical-table\">"));
        assertTrue(html.contains("<th>Target</th>"));
        assertTrue(html.contains("<td>R1</td>"));
    }
}
