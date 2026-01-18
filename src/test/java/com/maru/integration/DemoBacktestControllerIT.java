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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DemoBacktestController 통합테스트")
class DemoBacktestControllerIT {

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

    // ==================== 최적화 데모 ====================

    @Test
    @DisplayName("최적화 데모 페이지 - 성공")
    void optimizationPage_Success() throws Exception {
        mockMvc.perform(get("/trading/demo/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-demo"));
    }

    @Test
    @DisplayName("MA 크로스오버 최적화 - 성공")
    void runMACrossoverOptimization_Success() throws Exception {
        // Given
        Map<String, Object> result = createOptimizationResult("MA Crossover");
        when(tradingApiService.runMACrossoverOptimization()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/optimization/ma-crossover"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.strategy").value("MA Crossover"));
    }

    @Test
    @DisplayName("RSI 최적화 - 성공")
    void runRSIOptimization_Success() throws Exception {
        // Given
        Map<String, Object> result = createOptimizationResult("RSI");
        when(tradingApiService.runRSIOptimization()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/optimization/rsi"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.strategy").value("RSI"));
    }

    @Test
    @DisplayName("랜덤 서치 최적화 - 성공")
    void runRandomSearchOptimization_Success() throws Exception {
        // Given
        Map<String, Object> result = createOptimizationResult("Random Search");
        when(tradingApiService.runRandomSearchOptimization()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/optimization/random-search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.strategy").value("Random Search"));
    }

    // ==================== 데모 백테스트 ====================

    @Test
    @DisplayName("데모 백테스트 페이지 - 성공")
    void backtestDemoPage_Success() throws Exception {
        mockMvc.perform(get("/trading/demo/backtest"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-backtest"));
    }

    @Test
    @DisplayName("데모 데이터 생성 - 성공")
    void generateDemoData_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "데모 데이터가 생성되었습니다.");
        result.put("generatedRecords", 1000);
        when(tradingApiService.generateDemoData()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/backtest/generate-data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.generatedRecords").value(1000));
    }

    @Test
    @DisplayName("MA 크로스오버 백테스트 - 성공")
    void runMACrossoverBacktest_Success() throws Exception {
        // Given
        Map<String, Object> result = createBacktestResult("MA Crossover");
        when(tradingApiService.runMACrossoverBacktest()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/backtest/ma-crossover"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.strategy").value("MA Crossover"));
    }

    @Test
    @DisplayName("RSI 백테스트 - 성공")
    void runRSIBacktest_Success() throws Exception {
        // Given
        Map<String, Object> result = createBacktestResult("RSI");
        when(tradingApiService.runRSIBacktest()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/backtest/rsi"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.strategy").value("RSI"));
    }

    @Test
    @DisplayName("백테스트 비교 - 성공")
    void runBacktestComparison_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        List<Map<String, Object>> comparisons = new ArrayList<>();
        comparisons.add(createBacktestResult("MA Crossover"));
        comparisons.add(createBacktestResult("RSI"));
        result.put("comparisons", comparisons);
        when(tradingApiService.runBacktestComparison()).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo/backtest/compare"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comparisons").isArray());
    }

    @Test
    @DisplayName("데모 데이터 삭제 - 성공")
    void clearDemoData_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "데모 데이터가 삭제되었습니다.");
        when(tradingApiService.clearDemoData()).thenReturn(result);

        // When & Then
        mockMvc.perform(delete("/trading/demo/backtest/clear"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createOptimizationResult(String strategy) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("strategy", strategy);
        result.put("bestParameters", Map.of(
                "shortPeriod", 5,
                "longPeriod", 20,
                "threshold", 30
        ));
        result.put("sharpeRatio", new BigDecimal("1.85"));
        result.put("totalReturn", new BigDecimal("15.5"));
        result.put("maxDrawdown", new BigDecimal("-8.2"));
        return result;
    }

    private Map<String, Object> createBacktestResult(String strategy) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("strategy", strategy);
        result.put("totalTrades", 120);
        result.put("winRate", new BigDecimal("62.5"));
        result.put("totalProfitLoss", new BigDecimal("1500000"));
        result.put("sharpeRatio", new BigDecimal("1.45"));
        result.put("maxDrawdown", new BigDecimal("-350000"));
        result.put("profitFactor", new BigDecimal("1.68"));
        return result;
    }
}
