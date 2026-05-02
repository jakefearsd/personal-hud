package com.hud.analytics;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentExtractionSmokeTest {

    @Test
    void shouldExtractContentFromHtml() {
        String html = "<html><head><title>Actual Title</title></head><body>" +
                "<nav>Boilerplate Navbar</nav>" +
                "<main>" +
                "<h1>Actual Title</h1>" +
                "<p>This is the important content that should be extracted. It needs to be long enough for heuristics to prefer it over boilerplate.</p>" +
                "<p>Additional paragraph to increase the density of the main content area compared to the surrounding navigation and footer elements.</p>" +
                "</main>" +
                "<footer>Side Content</footer>" +
                "</body></html>";
        Readability4J readability4J = new Readability4J("https://example.com", html);
        Article article = readability4J.parse();

        assertNotNull(article);
        assertEquals("Actual Title", article.getTitle());
        assertTrue(article.getTextContent().contains("important content"));
        // Readability usually strips nav if it detects it as boilerplate
    }
}
