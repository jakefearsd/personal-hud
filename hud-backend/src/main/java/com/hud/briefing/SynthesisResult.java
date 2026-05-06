package com.hud.briefing;

/**
 * Encapsulates the result of an LLM synthesis operation, including the generated content
 * and token usage metrics.
 */
public record SynthesisResult(String content, int inputTokens, int outputTokens) {
    
    public static SynthesisResult empty() {
        return new SynthesisResult("", 0, 0);
    }
    
    public SynthesisResult plus(SynthesisResult other) {
        return new SynthesisResult(
            this.content + "\n" + other.content,
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens
        );
    }

    public SynthesisResult withCombinedContent(String newContent) {
        return new SynthesisResult(newContent, this.inputTokens, this.outputTokens);
    }
}
