package com.hud.briefing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BriefingPersonaTest {

    @Test
    void shouldReturnCorrectPersonaForCategory() {
        assertEquals("Geopolitical Strategist", BriefingPersona.of(BriefingCategory.WORLD_NEWS).name());
        assertEquals("Ukraine Ops Officer", BriefingPersona.of(BriefingCategory.THEATER_UKRAINE).name());
        assertEquals("Theater Command Analyst", BriefingPersona.of(BriefingCategory.GLOBAL_SITREP).name());
    }

    @Test
    void shouldHaveValidInstructions() {
        for (BriefingCategory category : BriefingCategory.values()) {
            BriefingPersona persona = BriefingPersona.of(category);
            assertNotNull(persona.name());
            assertNotNull(persona.instruction());
            assertNotNull(persona.focus());
        }
    }
}
