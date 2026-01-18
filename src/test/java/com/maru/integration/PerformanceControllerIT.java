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
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PerformanceController 통합테스트")
class PerformanceControllerIT {

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

    // ========== 성과 분석 메인 ==========

    @Test
    @DisplayName("성과 분석 메인 페이지 - 기본값")
    void performanceAnalysis_DefaultValues() throws Exception {
        // Given
        Map<String, Object> result = createPerformanceResult();
        when(tradingApiService.getPerformanceAnalysis(eq("daily"), anyString(), anyString(), isNull()))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attribute("period", "daily"))
                .andExpect(model().attributeExists("performanceData"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    @DisplayName("성과 분석 - 월별 집계")
    void performanceAnalysis_Monthly() throws Exception {
        // Given
        Map<String, Object> result = createPerformanceResult();
        when(tradingApiService.getPerformanceAnalysis("monthly", "2024-01-01", "2024-06-30", null))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance")
                        .param("period", "monthly")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attribute("period", "monthly"))
                .andExpect(model().attribute("startDate", "2024-01-01"))
                .andExpect(model().attribute("endDate", "2024-06-30"));
    }

    @Test
    @DisplayName("성과 분석 - 전략 필터")
    void performanceAnalysis_WithStrategyFilter() throws Exception {
        // Given
        Map<String, Object> result = createPerformanceResult();
        when(tradingApiService.getPerformanceAnalysis(eq("daily"), anyString(), anyString(), eq("strategy-1")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance")
                        .param("strategyId", "strategy-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attribute("strategyId", "strategy-1"));
    }

    @Test
    @DisplayName("성과 분석 - API 오류")
    void performanceAnalysis_ApiError() throws Exception {
        // Given
        when(tradingApiService.getPerformanceAnalysis(anyString(), anyString(), anyString(), isNull()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/performance"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/performance-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 전략별 통계 ==========

    @Test
    @DisplayName("전략별 통계 페이지 - 성공")
    void strategyStatistics_Success() throws Exception {
        // Given
        Map<String, Object> result = createStrategyStatisticsResult();
        when(tradingApiService.getStrategyStatistics(anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance/strategies"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/strategy-statistics"))
                .andExpect(model().attributeExists("strategies"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    @DisplayName("전략별 통계 - 기간 지정")
    void strategyStatistics_WithDateRange() throws Exception {
        // Given
        Map<String, Object> result = createStrategyStatisticsResult();
        when(tradingApiService.getStrategyStatistics("2024-01-01", "2024-06-30")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance/strategies")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/strategy-statistics"))
                .andExpect(model().attribute("startDate", "2024-01-01"))
                .andExpect(model().attribute("endDate", "2024-06-30"));
    }

    @Test
    @DisplayName("전략별 통계 - API 오류")
    void strategyStatistics_ApiError() throws Exception {
        // Given
        when(tradingApiService.getStrategyStatistics(anyString(), anyString()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/performance/strategies"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/strategy-statistics"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Excel 리포트 ==========

    @Test
    @DisplayName("Excel 리포트 다운로드 - 성공")
    void exportToExcel_Success() throws Exception {
        // Given
        Map<String, Object> result = createPerformanceResult();
        when(tradingApiService.getPerformanceAnalysis(eq("daily"), anyString(), anyString(), isNull()))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Excel 리포트 - 월별 데이터")
    void exportToExcel_Monthly() throws Exception {
        // Given
        Map<String, Object> result = createPerformanceResult();
        when(tradingApiService.getPerformanceAnalysis("monthly", "2024-01-01", "2024-06-30", null))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/performance/export")
                        .param("period", "monthly")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("Excel 리포트 - API 오류")
    void exportToExcel_ApiError() throws Exception {
        // Given
        when(tradingApiService.getPerformanceAnalysis(anyString(), anyString(), anyString(), isNull()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/performance/export"))
                .andExpect(status().isInternalServerError());
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createPerformanceResult() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> day1 = new HashMap<>();
        day1.put("date", "2024-01-01");
        day1.put("trades", 10);
        day1.put("profitLoss", new BigDecimal("150000"));
        day1.put("winRate", new BigDecimal("60"));
        day1.put("maxProfit", new BigDecimal("50000"));
        day1.put("maxLoss", new BigDecimal("-20000"));
        data.add(day1);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProfitLoss", new BigDecimal("1500000"));
        summary.put("totalTrades", 100);
        summary.put("winRate", new BigDecimal("58.5"));
        summary.put("avgProfitLoss", new BigDecimal("15000"));

        result.put("data", data);
        result.put("summary", summary);
        return result;
    }

    private Map<String, Object> createStrategyStatisticsResult() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("id", 1L);
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "테스트 전략");
        strategy.put("status", "ACTIVE");
        strategy.put("totalTrades", 50);
        strategy.put("winningTrades", 31);
        strategy.put("losingTrades", 19);
        strategy.put("winRate", new BigDecimal("62.5"));
        strategy.put("totalProfitLoss", new BigDecimal("750000"));
        strategy.put("avgProfitLoss", new BigDecimal("15000"));
        strategy.put("maxProfit", new BigDecimal("100000"));
        strategy.put("maxLoss", new BigDecimal("-50000"));
        strategies.add(strategy);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalStrategies", 1);
        summary.put("activeStrategies", 1);
        summary.put("totalTrades", 50);
        summary.put("totalProfitLoss", new BigDecimal("750000"));
        summary.put("avgWinRate", new BigDecimal("62.5"));

        result.put("strategies", strategies);
        result.put("summary", summary);
        return result;
    }
}
