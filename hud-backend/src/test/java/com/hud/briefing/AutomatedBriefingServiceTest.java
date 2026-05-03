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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class AutomatedBriefingServiceTest {

    @Mock private DynamicLlmService llmService;
    @Mock private DailyBriefingRepository briefingRepository;
    @Mock private BriefingProcessorFactory processorFactory;
    @Mock private PipelineRunRepository pipelineRunRepository;
    @Mock private TransactionTemplate transactionTemplate;

    private AutomatedBriefingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AutomatedBriefingService(llmService, briefingRepository, processorFactory, transactionTemplate, pipelineRunRepository);
        
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
        when(mockProcessor.process(anyString())).thenReturn("Content");
        
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
}
