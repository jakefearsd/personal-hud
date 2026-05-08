package com.hud.briefing;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class MarkdownService {

    private static final String CLASS_ATTR = "class";

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        List<org.commonmark.Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        );

        this.parser = Parser.builder()
                .extensions(extensions)
                .build();

        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .attributeProviderFactory(context -> new TacticalAttributeProvider())
                .build();
    }

    /**
     * Converts Markdown to sanitized HTML.
     * Injects tactical attributes like target="_blank" for links.
     */
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String normalized = normalizeMarkdown(markdown);
        Node document = parser.parse(normalized);
        String rawHtml = renderer.render(document);

        // Sanitize to prevent XSS while allowing tactical elements
        // Safelist.relaxed() allows tables, lists, links, etc.
        return Jsoup.clean(rawHtml, Safelist.relaxed()
                .addAttributes("a", "target", "rel", CLASS_ATTR)
                .addAttributes("table", CLASS_ATTR)
                .addTags("del"));
    }

    /**
     * Fixes common malformed Markdown patterns (e.g., squashed tables) 
     * to ensure the CommonMark parser recognizes them.
     */
    private String normalizeMarkdown(String markdown) {
        // 1. Ensure blank line before tables (lines starting with |)
        // 2. Fix 'squashed' tables where header and separator are on the same line
        // 3. Ensure tables aren't buried inside a paragraph without a leading newline
        
        String[] lines = markdown.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        boolean inTable = false;

        for (String line : lines) {
            String currentLine = line;
            String trimmedLine = currentLine.trim();
            
            // Detect squashed header/separator: "| a | b | | :--- | :--- |"
            if (trimmedLine.contains("| | :") || trimmedLine.contains("| | :---")) {
                currentLine = currentLine.replace("| | :", "|\n| :");
                trimmedLine = currentLine.trim();
            }

            if (trimmedLine.startsWith("|")) {
                // If the previous line wasn't blank and didn't start with |, add a newline
                if (!inTable && sb.length() > 0 && !sb.toString().endsWith("\n\n")) {
                    sb.append("\n");
                }
                inTable = true;
            } else {
                inTable = false;
            }
            
            sb.append(currentLine).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Injects standard tactical attributes into HTML elements.
     */
    private static final class TacticalAttributeProvider implements AttributeProvider {
        private static final String TAG_A = "a";
        private static final String TAG_TABLE = "table";

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (TAG_A.equals(tagName)) {
                attributes.put("target", "_blank");
                attributes.put("rel", "noopener noreferrer");
                attributes.put(CLASS_ATTR, "tactical-link");
            }
            if (TAG_TABLE.equals(tagName)) {
                attributes.put(CLASS_ATTR, "tactical-table");
            }
        }
    }
}
