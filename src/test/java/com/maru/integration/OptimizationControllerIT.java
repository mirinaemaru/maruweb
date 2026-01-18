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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OptimizationController 통합테스트")
class OptimizationControllerIT {

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

    // ========== 파라미터 최적화 페이지 ==========

    @Test
    @DisplayName("최적화 메인 페이지 - 성공")
    void optimizationPage_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategies", createStrategiesList());
        when(tradingApiService.getStrategies()).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("최적화 메인 페이지 - 전략 선택")
    void optimizationPage_WithStrategyId() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategies", createStrategiesList());
        when(tradingApiService.getStrategies()).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/optimization")
                        .param("strategyId", "strategy-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization"))
                .andExpect(model().attribute("selectedStrategyId", "strategy-1"));
    }

    @Test
    @DisplayName("최적화 메인 페이지 - API 오류")
    void optimizationPage_ApiError() throws Exception {
        // Given
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 그리드 서치 (AJAX) ==========

    @Test
    @DisplayName("그리드 서치 실행 - 성공")
    void runGridSearch_Success() throws Exception {
        // Given
        Map<String, Object> result = createGridSearchResult();
        when(tradingApiService.runGridSearch(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/optimization/grid-search")
                        .param("strategyId", "strategy-1")
                        .param("parameterName", "movingAveragePeriod")
                        .param("startValue", "5")
                        .param("endValue", "50")
                        .param("stepValue", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("그리드 서치 실행 - 기간 지정")
    void runGridSearch_WithDateRange() throws Exception {
        // Given
        Map<String, Object> result = createGridSearchResult();
        when(tradingApiService.runGridSearch(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/optimization/grid-search")
                        .param("strategyId", "strategy-1")
                        .param("parameterName", "rsiThreshold")
                        .param("startValue", "20")
                        .param("endValue", "80")
                        .param("stepValue", "10")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("그리드 서치 실행 - API 오류")
    void runGridSearch_ApiError() throws Exception {
        // Given
        when(tradingApiService.runGridSearch(anyMap())).thenThrow(new RuntimeException("최적화 실패"));

        // When & Then
        mockMvc.perform(post("/trading/optimization/grid-search")
                        .param("strategyId", "strategy-1")
                        .param("parameterName", "movingAveragePeriod")
                        .param("startValue", "5")
                        .param("endValue", "50")
                        .param("stepValue", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== 최적화 결과 ==========

    @Test
    @DisplayName("최적화 결과 조회 - 성공")
    void optimizationResults_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("results", createOptimizationResultsList());
        when(tradingApiService.getOptimizationResults(null)).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/optimization/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-results"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    @DisplayName("최적화 결과 조회 - 전략 필터")
    void optimizationResults_WithStrategyFilter() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("results", createOptimizationResultsList());
        when(tradingApiService.getOptimizationResults("strategy-1")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/optimization/results")
                        .param("strategyId", "strategy-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-results"))
                .andExpect(model().attribute("strategyId", "strategy-1"));
    }

    @Test
    @DisplayName("최적화 결과 조회 - API 오류")
    void optimizationResults_ApiError() throws Exception {
        // Given
        when(tradingApiService.getOptimizationResults(isNull()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/optimization/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-results"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 유전 알고리즘 (AJAX) ==========

    @Test
    @DisplayName("유전 알고리즘 실행 - 성공")
    void runGeneticAlgorithm_Success() throws Exception {
        // Given
        Map<String, Object> result = createGeneticAlgorithmResult();
        when(tradingApiService.runGeneticAlgorithm(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/optimization/genetic-algorithm")
                        .param("strategyId", "strategy-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.bestParameters").exists());
    }

    @Test
    @DisplayName("유전 알고리즘 실행 - 커스텀 파라미터")
    void runGeneticAlgorithm_WithCustomParams() throws Exception {
        // Given
        Map<String, Object> result = createGeneticAlgorithmResult();
        when(tradingApiService.runGeneticAlgorithm(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/optimization/genetic-algorithm")
                        .param("strategyId", "strategy-1")
                        .param("populationSize", "200")
                        .param("generations", "100")
                        .param("mutationRate", "0.05")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("유전 알고리즘 실행 - API 오류")
    void runGeneticAlgorithm_ApiError() throws Exception {
        // Given
        when(tradingApiService.runGeneticAlgorithm(anyMap()))
                .thenThrow(new RuntimeException("유전 알고리즘 실행 실패"));

        // When & Then
        mockMvc.perform(post("/trading/optimization/genetic-algorithm")
                        .param("strategyId", "strategy-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== Helper Methods ==========

    private List<Map<String, Object>> createStrategiesList() {
        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> strategy1 = new HashMap<>();
        strategy1.put("id", 1L);  // 템플릿에서 사용
        strategy1.put("strategyId", "strategy-1");
        strategy1.put("name", "테스트 전략 1");
        strategies.add(strategy1);

        Map<String, Object> strategy2 = new HashMap<>();
        strategy2.put("id", 2L);  // 템플릿에서 사용
        strategy2.put("strategyId", "strategy-2");
        strategy2.put("name", "테스트 전략 2");
        strategies.add(strategy2);
        return strategies;
    }

    private Map<String, Object> createGridSearchResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 5; i <= 50; i += 5) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("parameterValue", i);
            entry.put("profitLoss", new BigDecimal(i * 1000));
            entry.put("sharpeRatio", new BigDecimal("1." + (i / 10)));
            entry.put("maxDrawdown", new BigDecimal("-" + (100 - i) * 100));
            results.add(entry);
        }

        result.put("results", results);
        result.put("bestValue", 25);
        result.put("bestProfitLoss", new BigDecimal("250000"));
        return result;
    }

    private List<Map<String, Object>> createOptimizationResultsList() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result1 = new HashMap<>();
        result1.put("optimizationId", "opt-1");
        result1.put("strategyId", "strategy-1");
        result1.put("parameterName", "movingAveragePeriod");
        result1.put("parameterValue", 20);  // 템플릿에서 사용
        result1.put("bestValue", 20);
        result1.put("profitLoss", new BigDecimal("500000"));
        result1.put("winRate", new BigDecimal("65.5"));  // 템플릿에서 사용
        result1.put("totalTrades", 100);  // 템플릿에서 사용
        result1.put("winningTrades", 66);  // 템플릿에서 사용
        result1.put("losingTrades", 34);  // 템플릿에서 사용
        result1.put("sharpeRatio", new BigDecimal("1.85"));  // 템플릿에서 사용
        result1.put("createdAt", "2024-01-01T10:00:00");
        results.add(result1);
        return results;
    }

    private Map<String, Object> createGeneticAlgorithmResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("generations", 50);
        result.put("finalFitness", new BigDecimal("0.85"));

        Map<String, Object> bestParameters = new HashMap<>();
        bestParameters.put("movingAveragePeriod", 21);
        bestParameters.put("rsiThreshold", 35);
        bestParameters.put("stopLossPercent", 2.5);
        result.put("bestParameters", bestParameters);

        result.put("profitLoss", new BigDecimal("750000"));
        result.put("sharpeRatio", new BigDecimal("1.85"));
        return result;
    }
}
