package com.hud.briefing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class SynthesisResultTest {

    @Test
    void empty_returnsEmptyResult() {
        SynthesisResult result = SynthesisResult.empty();
        assertEquals("", result.content());
        assertEquals(0, result.inputTokens());
        assertEquals(0, result.outputTokens());
    }

    @Test
    void plus_combinesResults() {
        SynthesisResult result1 = new SynthesisResult("Part 1", 10, 5);
        SynthesisResult result2 = new SynthesisResult("Part 2", 15, 8);
        
        SynthesisResult combined = result1.plus(result2);
        
        assertEquals("Part 1\nPart 2", combined.content());
        assertEquals(25, combined.inputTokens());
        assertEquals(13, combined.outputTokens());
    }

    @Test
    void withCombinedContent_updatesContentOnly() {
        SynthesisResult original = new SynthesisResult("Original", 10, 5);
        
        SynthesisResult updated = original.withCombinedContent("New Content");
        
        assertEquals("New Content", updated.content());
        assertEquals(10, updated.inputTokens());
        assertEquals(5, updated.outputTokens());
    }
}
