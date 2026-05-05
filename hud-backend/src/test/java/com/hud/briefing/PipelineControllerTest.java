package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PipelineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PipelineRunRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PipelineController controller = new PipelineController(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldGetRecentRuns() throws Exception {
        when(repository.findTop50ByOrderByStartTimeDesc()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pipelines"))
                .andExpect(status().isOk());

        verify(repository).findTop50ByOrderByStartTimeDesc();
    }

    @Test
    void shouldFlushAllRuns() throws Exception {
        mockMvc.perform(delete("/api/pipelines"))
                .andExpect(status().isOk());

        verify(repository).deleteAll();
    }
}
