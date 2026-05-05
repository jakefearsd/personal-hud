package com.hud.analytics;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRenderingSmokeTest {

    @Test
    void shouldRenderMarkdownWithExtensions() {
        String markdown = "# Tactical Briefing\n" +
                "\n" +
                "This is a **smoke test** for the Markdown renderer.\n" +
                "It supports ~~strikethrough~~ and autolinks: https://google.com\n" +
                "\n" +
                "| Metric | Status |\n" +
                "| --- | --- |\n" +
                "| Readiness | High |\n" +
                "| Deployment | Active |";

        List<org.commonmark.Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        );

        Parser parser = Parser.builder()
                .extensions(extensions)
                .build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();

        String html = renderer.render(document);

        assertTrue(html.contains("<h1>Tactical Briefing</h1>"));
        assertTrue(html.contains("<strong>smoke test</strong>"));
        assertTrue(html.contains("<del>strikethrough</del>"));
        assertTrue(html.contains("<a href=\"https://google.com\">https://google.com</a>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>Metric</th>"));
    }
}
