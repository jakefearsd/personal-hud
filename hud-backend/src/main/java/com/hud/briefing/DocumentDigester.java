package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Map stage of the briefing pipeline. Turns one scraped document into a compact
 * {@link ArticleDigest} via a single bounded LLM call. The digest is small, so
 * every document a run scrapes can later be fed to one reduce-stage prompt.
 */
@Component
public class DocumentDigester {

    /** Upper bound on document text sent to the model, to keep each map call bounded. */
    private static final int MAX_DOC_CHARS = 200_000;
    private static final double DEFAULT_RELEVANCE = 0.5;

    /** Map-stage result: the digest plus the token cost of producing it. */
    public record DigestResult(ArticleDigest digest, int inputTokens, int outputTokens) {}

    public DigestResult digest(ChatLanguageModel model, String documentText,
                               SourceLink source, String relevanceContext) {
        String body = documentText == null ? "" : documentText;
        if (body.length() > MAX_DOC_CHARS) {
            body = body.substring(0, MAX_DOC_CHARS);
        }
        String prompt = buildPrompt(body, relevanceContext);

        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        ArticleDigest digest = parseDigest(response.content().text(), source);

        return new DigestResult(
                digest,
                usage != null ? usage.inputTokenCount() : 0,
                usage != null ? usage.outputTokenCount() : 0);
    }

    static String buildPrompt(String documentText, String relevanceContext) {
        return "You are an intelligence analyst. Read the SOURCE DOCUMENT and extract a structured digest.\n"
                + "Relevance context: " + relevanceContext + "\n"
                + "Respond ONLY in this exact format, with no extra commentary:\n\n"
                + "HEADLINE: <one concise line naming the core event>\n"
                + "RELEVANCE: <number 0.0 to 1.0 — relevance to the context above>\n"
                + "SUMMARY: <2-4 sentences of dense factual summary>\n"
                + "KEY FACTS:\n- <fact>\n- <fact>\n"
                + "ENTITIES: <comma-separated people, organizations, places>\n"
                + "DATED EVENTS:\n- <date>: <what happened>\n\n"
                + "SOURCE DOCUMENT:\n" + documentText;
    }

    /**
     * Lenient parser for the digester's labelled-section format. Unknown lines
     * are treated as continuations of the current section; missing fields fall
     * back to safe defaults so a malformed model response never fails the run.
     */
    static ArticleDigest parseDigest(String llmText, SourceLink source) {
        String headline = "";
        double relevance = DEFAULT_RELEVANCE;
        StringBuilder summary = new StringBuilder();
        List<String> keyFacts = new ArrayList<>();
        List<String> entities = new ArrayList<>();
        List<String> datedEvents = new ArrayList<>();

        String section = "";
        if (llmText != null) {
            for (String raw : llmText.split("\\r?\\n")) {
                String line = raw.strip();
                if (line.isEmpty()) continue;
                String upper = line.toUpperCase(Locale.ROOT);

                if (upper.startsWith("HEADLINE:")) {
                    headline = valueAfterColon(line);
                    section = "HEADLINE";
                } else if (upper.startsWith("RELEVANCE:")) {
                    relevance = parseRelevance(valueAfterColon(line));
                    section = "RELEVANCE";
                } else if (upper.startsWith("SUMMARY:")) {
                    appendText(summary, valueAfterColon(line));
                    section = "SUMMARY";
                } else if (upper.startsWith("KEY FACTS:")) {
                    section = "KEY_FACTS";
                } else if (upper.startsWith("ENTITIES:")) {
                    addCsv(entities, valueAfterColon(line));
                    section = "ENTITIES";
                } else if (upper.startsWith("DATED EVENTS:")) {
                    section = "DATED_EVENTS";
                } else {
                    switch (section) {
                        case "SUMMARY"      -> appendText(summary, line);
                        case "KEY_FACTS"    -> addBullet(keyFacts, line);
                        case "DATED_EVENTS" -> addBullet(datedEvents, line);
                        case "ENTITIES"     -> addCsv(entities, line);
                        default             -> { /* ignore stray pre-amble */ }
                    }
                }
            }
        }
        if (headline.isBlank()) {
            headline = source.sourceName();
        }
        return new ArticleDigest(
                source.url(), source.sourceName(), source.tier(),
                headline, summary.toString().strip(),
                List.copyOf(keyFacts), List.copyOf(entities), List.copyOf(datedEvents),
                relevance, 1, List.of(source.sourceName()));
    }

    private static String valueAfterColon(String line) {
        int idx = line.indexOf(':');
        return idx >= 0 ? line.substring(idx + 1).strip() : "";
    }

    private static double parseRelevance(String value) {
        try {
            double parsed = Double.parseDouble(value.strip());
            return Math.max(0.0, Math.min(1.0, parsed));
        } catch (NumberFormatException e) {
            return DEFAULT_RELEVANCE;
        }
    }

    private static void appendText(StringBuilder sb, String text) {
        if (text == null || text.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(text.strip());
    }

    private static void addBullet(List<String> target, String line) {
        String cleaned = line.replaceFirst("^[-*\\u2022]\\s*", "").strip();
        if (!cleaned.isEmpty()) target.add(cleaned);
    }

    private static void addCsv(List<String> target, String csv) {
        for (String part : csv.split(",")) {
            String t = part.strip();
            if (!t.isEmpty()) target.add(t);
        }
    }
}
