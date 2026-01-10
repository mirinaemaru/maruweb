package com.maru.trading.controller;

import com.maru.trading.controller.TestConfig;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(PerformanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PerformanceController 단위 테스트")
class PerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockPerformanceResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", Arrays.asList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProfitLoss", 100000);
        summary.put("totalTrades", 50);
        summary.put("winRate", 65.0);
        summary.put("avgProfitLoss", 2000);
        result.put("summary", summary);
        return result;
    }

    private Map<String, Object> createMockStrategyStatsResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("strategies", Arrays.asList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalStrategies", 5);
        summary.put("activeStrategies", 3);
        summary.put("totalTrades", 100);
        summary.put("totalProfitLoss", 250000);
        summary.put("avgWinRate", 60.0);
        result.put("summary", summary);
        return result;
    }

    @Test
    @DisplayName("성과 분석 페이지 - 성공")
    void performance_Success() throws Exception {
        when(tradingApiService.getPerformanceAnalysis(any(), any(), any(), any()))
                .thenReturn(createMockPerformanceResponse());

        mockMvc.perform(get("/trading/performance"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attributeExists("period"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @DisplayName("전략별 통계 페이지 - 성공")
    void strategyStats_Success() throws Exception {
        when(tradingApiService.getStrategyStatistics(any(), any()))
                .thenReturn(createMockStrategyStatsResponse());

        mockMvc.perform(get("/trading/performance/strategies"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/strategy-statistics"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("성과 분석 - API 오류 시")
    void performance_ApiError() throws Exception {
        when(tradingApiService.getPerformanceAnalysis(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/performance"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attributeExists("error"));
    }
}
