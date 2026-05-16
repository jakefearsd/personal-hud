package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
class DocumentDigesterTest {

    @Mock private ChatLanguageModel model;
    private DocumentDigester digester;

    private static final SourceLink SOURCE =
            new SourceLink("http://news/article-1", "BBC World", SourceTier.TIER_1);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        digester = new DocumentDigester();
    }

    @Test
    void parsesAWellFormedDigest() {
        String llm = String.join("\n",
                "HEADLINE: Major escalation reported",
                "RELEVANCE: 0.9",
                "SUMMARY: Forces advanced overnight.",
                "Heavy shelling continued at dawn.",
                "KEY FACTS:",
                "- 12 strikes recorded",
                "- supply line cut",
                "ENTITIES: Kyiv, NATO, Black Sea",
                "DATED EVENTS:",
                "- 2026-05-14: counteroffensive began");

        ArticleDigest d = DocumentDigester.parseDigest(llm, SOURCE);

        assertEquals("Major escalation reported", d.headline());
        assertEquals(0.9, d.relevanceScore(), 0.0001);
        assertEquals("Forces advanced overnight. Heavy shelling continued at dawn.", d.summary());
        assertEquals(2, d.keyFacts().size());
        assertEquals(3, d.entities().size());
        assertEquals(1, d.datedEvents().size());
        assertEquals("http://news/article-1", d.sourceUrl());
        assertEquals(SourceTier.TIER_1, d.tier());
        assertEquals(1, d.corroborationCount());
        assertEquals(java.util.List.of("BBC World"), d.contributingSources());
    }

    @Test
    void appliesSafeDefaultsForMissingOrUnparseableFields() {
        ArticleDigest d = DocumentDigester.parseDigest("totally unstructured model babble", SOURCE);

        assertEquals("BBC World", d.headline());      // falls back to source name
        assertEquals(0.5, d.relevanceScore(), 0.0001); // default relevance
        assertEquals("", d.summary());
        assertTrue(d.keyFacts().isEmpty());
        assertTrue(d.entities().isEmpty());
    }

    @Test
    void clampsRelevanceIntoUnitRange() {
        ArticleDigest high = DocumentDigester.parseDigest("RELEVANCE: 7.5", SOURCE);
        ArticleDigest low = DocumentDigester.parseDigest("RELEVANCE: -3", SOURCE);
        assertEquals(1.0, high.relevanceScore(), 0.0001);
        assertEquals(0.0, low.relevanceScore(), 0.0001);
    }

    @Test
    void digestCallsTheModelAndReportsTokenUsage() {
        Response<AiMessage> response = Response.from(
                AiMessage.from("HEADLINE: Test\nSUMMARY: A summary."),
                new TokenUsage(120, 40));
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        DocumentDigester.DigestResult result =
                digester.digest(model, "Some long document body.", SOURCE, "national security");

        assertEquals("Test", result.digest().headline());
        assertEquals(120, result.inputTokens());
        assertEquals(40, result.outputTokens());
    }
}
