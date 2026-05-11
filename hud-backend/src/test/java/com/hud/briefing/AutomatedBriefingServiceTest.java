package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class AutomatedBriefingServiceTest {

    @Mock private DynamicLlmService llmService;
    @Mock private DailyBriefingRepository briefingRepository;
    @Mock private BriefingProcessorFactory processorFactory;
    @Mock private PipelineRunRepository pipelineRunRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private MarkdownService markdownService;
    @Mock private com.hud.news.PredictionService predictionService;

    private AutomatedBriefingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AutomatedBriefingService(llmService, briefingRepository, processorFactory, transactionTemplate, pipelineRunRepository, markdownService, predictionService);
        
        // Mock the TransactionTemplate to execute the action immediately
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void shouldTriggerBriefingForAllActiveModels() {
        // Arrange
        DynamicLlmService.NamedChatModel model = new DynamicLlmService.NamedChatModel("Gemma", null);
        when(llmService.getActiveModels()).thenReturn(List.of(model));
        
        BriefingProcessor mockProcessor = mock(BriefingProcessor.class);
        when(processorFactory.getProcessor(any(), any())).thenReturn(mockProcessor);
        when(mockProcessor.process(anyString())).thenReturn(new SynthesisResult("Content", 100, 10));
        
        when(pipelineRunRepository.save(any())).thenReturn(new PipelineRun());
        when(pipelineRunRepository.findById(any())).thenReturn(Optional.of(new PipelineRun()));

        // Act
        service.generateDailyBriefing();

        // Assert
        verify(llmService).getActiveModels();
        // There are 7 categories in BriefingCategory
        verify(processorFactory, times(7)).getProcessor(any(), any());
        verify(briefingRepository, times(7)).save(any());
    }

    @Test
    void shouldTriggerBriefingForSpecificModel() {
        LlmConfig config = new LlmConfig("Specific", LlmProvider.OLLAMA, "gemma", true);
        config.setId(1L);
        LlmConfigRepository mockRepo = mock(LlmConfigRepository.class);
        when(llmService.getConfigRepository()).thenReturn(mockRepo);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(config));
        
        DynamicLlmService.NamedChatModel model = new DynamicLlmService.NamedChatModel("Specific", null);
        when(llmService.buildSpecificModel(config)).thenReturn(model);
        
        BriefingProcessor mockProcessor = mock(BriefingProcessor.class);
        when(processorFactory.getProcessor(any(), any())).thenReturn(mockProcessor);
        when(mockProcessor.process(anyString())).thenReturn(new SynthesisResult("Content", 100, 10));
        when(pipelineRunRepository.save(any())).thenReturn(new PipelineRun());
        when(pipelineRunRepository.findById(any())).thenReturn(Optional.of(new PipelineRun()));

        service.generateForModel(1L);

        verify(llmService).buildSpecificModel(config);
        verify(briefingRepository, times(7)).save(any());
    }

    @Test
    void shouldHandleInactiveModelSpecificRun() {
        LlmConfig config = new LlmConfig("Inactive", LlmProvider.OLLAMA, "gemma", false);
        config.setId(1L);
        LlmConfigRepository mockRepo = mock(LlmConfigRepository.class);
        when(llmService.getConfigRepository()).thenReturn(mockRepo);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(config));

        service.generateForModel(1L);

        verify(llmService, never()).buildSpecificModel(any());
    }

    @Test
    void shouldGenerateForSpecificCategory() {
        DynamicLlmService.NamedChatModel model = new DynamicLlmService.NamedChatModel("Gemma", null);
        when(llmService.getActiveModels()).thenReturn(List.of(model));
        
        BriefingProcessor mockProcessor = mock(BriefingProcessor.class);
        when(processorFactory.getProcessor(any(), any())).thenReturn(mockProcessor);
        when(mockProcessor.process(anyString())).thenReturn(new SynthesisResult("Content", 100, 10));
        when(pipelineRunRepository.save(any())).thenReturn(new PipelineRun());
        when(pipelineRunRepository.findById(any())).thenReturn(Optional.of(new PipelineRun()));

        service.generateForCategory(BriefingCategory.FINANCE);

        verify(processorFactory).getProcessor(eq(BriefingCategory.FINANCE), any());
        verify(briefingRepository).save(any());
    }

    @Test
    void shouldHandleProcessingFailure() {
        DynamicLlmService.NamedChatModel model = new DynamicLlmService.NamedChatModel("Gemma", null);
        BriefingProcessor mockProcessor = mock(BriefingProcessor.class);
        when(processorFactory.getProcessor(any(), any())).thenReturn(mockProcessor);
        when(mockProcessor.process(anyString())).thenThrow(new RuntimeException("API Error"));
        
        PipelineRun run = new PipelineRun();
        run.setId(99L);
        when(pipelineRunRepository.save(any())).thenReturn(run);
        when(pipelineRunRepository.findById(99L)).thenReturn(Optional.of(run));

        try {
            service.generateForCategory(LocalDate.now(), BriefingCategory.WORLD_NEWS, "query", model);
        } catch (Exception e) {
            // expected
        }

        assertEquals(PipelineStatus.FAILED, run.getStatus());
        assertEquals("API Error", run.getErrorMessage());
    }
}
