package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(ExecutionHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ExecutionHistoryController 단위 테스트")
class ExecutionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockHistoryResponse() {
        Map<String, Object> historyResponse = new HashMap<>();
        historyResponse.put("executions", Arrays.asList());
        historyResponse.put("totalExecutions", 0);
        historyResponse.put("successfulExecutions", 0);
        historyResponse.put("failedExecutions", 0);
        historyResponse.put("totalProfitLoss", 0);
        return historyResponse;
    }

    private Map<String, Object> createMockStrategiesResponse() {
        Map<String, Object> strategiesResponse = new HashMap<>();
        strategiesResponse.put("items", Collections.emptyList());
        return strategiesResponse;
    }

    @Test
    @DisplayName("실행 히스토리 페이지 - 성공")
    void history_Success() throws Exception {
        when(tradingApiService.getStrategies()).thenReturn(createMockStrategiesResponse());
        when(tradingApiService.getExecutionHistory(any(), any(), any(), any()))
                .thenReturn(createMockHistoryResponse());

        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attributeExists("strategies"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @DisplayName("실행 히스토리 - 전략 필터")
    void history_WithStrategyFilter() throws Exception {
        // Mock strategy data from API
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-001");
        strategy.put("name", "테스트 전략");
        strategy.put("status", "ACTIVE");

        Map<String, Object> strategiesResponse = new HashMap<>();
        strategiesResponse.put("items", Arrays.asList(strategy));
        when(tradingApiService.getStrategies()).thenReturn(strategiesResponse);

        Map<String, Object> historyResponse = new HashMap<>();
        Map<String, Object> execution1 = new HashMap<>();
        execution1.put("executionId", "exec-001");
        execution1.put("strategyId", "strategy-001");
        execution1.put("strategyName", "테스트 전략");
        execution1.put("status", "SUCCESS");
        execution1.put("profitLoss", 10000);
        execution1.put("executedAt", LocalDateTime.of(2025, 6, 1, 10, 0, 0));
        execution1.put("symbol", "005930");
        execution1.put("side", "BUY");
        execution1.put("quantity", 100);
        execution1.put("price", 70000);
        execution1.put("message", "");
        execution1.put("errorMessage", null);
        historyResponse.put("executions", Arrays.asList(execution1));
        historyResponse.put("totalExecutions", 1);
        historyResponse.put("successfulExecutions", 1);
        historyResponse.put("failedExecutions", 0);
        historyResponse.put("totalProfitLoss", 10000);

        when(tradingApiService.getExecutionHistory(any(), any(), any(), any()))
                .thenReturn(historyResponse);

        mockMvc.perform(get("/trading/execution-history")
                        .param("strategyId", "strategy-001")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attributeExists("executions"))
                .andExpect(model().attribute("strategyId", "strategy-001"));
    }

    @Test
    @DisplayName("실행 히스토리 - API 오류 시")
    void history_ApiError() throws Exception {
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/execution-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/execution-history"))
                .andExpect(model().attributeExists("error"));
    }
}
