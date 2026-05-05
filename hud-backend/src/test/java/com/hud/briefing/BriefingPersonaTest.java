package com.hud.briefing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BriefingPersonaTest {

    @Test
    void shouldReturnCorrectPersonaForCategory() {
        assertEquals("Geopolitical Strategist", BriefingCategory.WORLD_NEWS.getPersona().name());
        assertEquals("Ukraine Ops Officer", BriefingCategory.THEATER_UKRAINE.getPersona().name());
        assertEquals("Theater Command Analyst", BriefingCategory.GLOBAL_SITREP.getPersona().name());
    }

    @Test
    void shouldHaveValidInstructions() {
        for (BriefingCategory category : BriefingCategory.values()) {
            BriefingPersona persona = category.getPersona();
            assertNotNull(persona.name());
            assertNotNull(persona.instruction());
            assertNotNull(persona.focus());
        }
    }
}
