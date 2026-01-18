package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ExecutionHistoryController 통합테스트")
class ExecutionHistoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    @Test
    @DisplayName("실행 히스토리 페이지 - 기본값")
    void page_DefaultValues() throws Exception {
        // Given
        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", createStrategiesList());
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        Map<String, Object> executionResult = createExecutionHistoryResult();
        when(tradingApiService.getExecutionHistory(isNull(), anyString(), anyString(), isNull()))
                .thenReturn(executionResult);

        // When & Then
        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"))
                .andExpect(model().attributeExists("executions"))
                .andExpect(model().attributeExists("totalExecutions"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("실행 히스토리 - 필터 적용")
    void page_WithFilters() throws Exception {
        // Given
        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", createStrategiesList());
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        Map<String, Object> executionResult = createExecutionHistoryResult();
        when(tradingApiService.getExecutionHistory("strategy-1", "2024-01-01", "2024-06-30", "SUCCESS"))
                .thenReturn(executionResult);

        // When & Then
        mockMvc.perform(get("/trading/execution-history")
                        .param("strategyId", "strategy-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attribute("strategyId", "strategy-1"))
                .andExpect(model().attribute("startDate", "2024-01-01"))
                .andExpect(model().attribute("endDate", "2024-06-30"))
                .andExpect(model().attribute("status", "SUCCESS"));
    }

    @Test
    @DisplayName("실행 히스토리 - API 타임아웃")
    void page_ApiTimeout() throws Exception {
        // Given
        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", createStrategiesList());
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        when(tradingApiService.getExecutionHistory(isNull(), anyString(), anyString(), isNull()))
                .thenThrow(new RuntimeException("Request timeout"));

        // When & Then
        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attributeExists("warning"))
                .andExpect(model().attribute("totalExecutions", 0));
    }

    @Test
    @DisplayName("실행 히스토리 - 전략 목록 조회 실패")
    void page_StrategyFetchFailed() throws Exception {
        // Given
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("API error"));

        Map<String, Object> executionResult = createExecutionHistoryResult();
        when(tradingApiService.getExecutionHistory(isNull(), anyString(), anyString(), isNull()))
                .thenReturn(executionResult);

        // When & Then
        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attribute("strategies", Collections.emptyList()));
    }

    @Test
    @DisplayName("실행 히스토리 - 전략 및 실행 히스토리 모두 조회 실패")
    void page_CompleteFailure() throws Exception {
        // Given - 두 API 모두 실패해도 내부 try-catch로 처리됨
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Fatal error"));
        when(tradingApiService.getExecutionHistory(isNull(), anyString(), anyString(), isNull()))
                .thenThrow(new RuntimeException("Fatal error"));

        // When & Then - 내부 try-catch가 처리하여 warning 표시 (error 아님)
        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attribute("strategies", Collections.emptyList()))
                .andExpect(model().attributeExists("warning"))
                .andExpect(model().attribute("totalExecutions", 0));
    }

    // ========== Helper Methods ==========

    private List<Map<String, Object>> createStrategiesList() {
        List<Map<String, Object>> strategies = new ArrayList<>();

        Map<String, Object> s1 = new HashMap<>();
        s1.put("id", 1L);
        s1.put("strategyId", "strategy-1");
        s1.put("name", "테스트 전략 1");
        s1.put("title", "테스트 전략 1");  // 템플릿에서 사용
        strategies.add(s1);

        Map<String, Object> s2 = new HashMap<>();
        s2.put("id", 2L);
        s2.put("strategyId", "strategy-2");
        s2.put("name", "테스트 전략 2");
        s2.put("title", "테스트 전략 2");  // 템플릿에서 사용
        strategies.add(s2);

        return strategies;
    }

    private Map<String, Object> createExecutionHistoryResult() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> executions = new ArrayList<>();

        Map<String, Object> e1 = new HashMap<>();
        e1.put("executionId", "exec-1");
        e1.put("strategyId", "strategy-1");
        e1.put("strategyName", "테스트 전략 1");
        e1.put("symbol", "005930");
        e1.put("side", "BUY");
        e1.put("quantity", 10);
        e1.put("price", new BigDecimal("75000"));
        e1.put("status", "SUCCESS");
        e1.put("profitLoss", new BigDecimal("50000"));
        e1.put("message", "실행 완료");
        e1.put("executedAt", LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        executions.add(e1);

        Map<String, Object> e2 = new HashMap<>();
        e2.put("executionId", "exec-2");
        e2.put("strategyId", "strategy-1");
        e2.put("strategyName", "테스트 전략 1");
        e2.put("symbol", "005930");
        e2.put("side", "SELL");
        e2.put("quantity", 10);
        e2.put("price", new BigDecimal("80000"));
        e2.put("status", "SUCCESS");
        e2.put("profitLoss", new BigDecimal("50000"));
        e2.put("message", "실행 완료");
        e2.put("executedAt", LocalDateTime.of(2024, 1, 16, 14, 20, 0));
        executions.add(e2);

        result.put("executions", executions);
        result.put("totalExecutions", 2);
        result.put("successfulExecutions", 2);
        result.put("failedExecutions", 0);
        result.put("totalProfitLoss", new BigDecimal("100000"));

        return result;
    }
}
