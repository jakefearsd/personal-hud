package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BriefingSourceFactoryTest {

    @Mock private BriefingSourceStrategy strategy1;
    @Mock private BriefingSourceStrategy strategy2;

    private BriefingSourceFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new BriefingSourceFactory(List.of(strategy1, strategy2));
    }

    @Test
    void shouldReturnCorrectStrategy() {
        when(strategy1.supports(BriefingCategory.FINANCE)).thenReturn(true);
        when(strategy2.supports(BriefingCategory.FINANCE)).thenReturn(false);

        BriefingSourceStrategy result = factory.getStrategy(BriefingCategory.FINANCE);
        assertEquals(strategy1, result);
    }

    @Test
    void shouldThrowExceptionWhenNoStrategyFound() {
        when(strategy1.supports(any())).thenReturn(false);
        when(strategy2.supports(any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> factory.getStrategy(BriefingCategory.WORLD_NEWS));
    }
}
