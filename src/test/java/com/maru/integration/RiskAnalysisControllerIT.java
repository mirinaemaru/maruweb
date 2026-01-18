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
@DisplayName("RiskAnalysisController 통합테스트")
class RiskAnalysisControllerIT {

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

    // ========== VaR 분석 ==========

    @Test
    @DisplayName("VaR 분석 페이지 - 기본값")
    void varAnalysis_DefaultValues() throws Exception {
        // Given
        Map<String, Object> result = createVaRResult();
        when(tradingApiService.calculateVaR(isNull(), eq(0.95), eq(1), anyString(), anyString()))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/risk/var"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/var-analysis"))
                .andExpect(model().attribute("confidenceLevel", 0.95))
                .andExpect(model().attribute("timeHorizon", 1))
                .andExpect(model().attributeExists("varData"))
                .andExpect(model().attributeExists("statistics"));
    }

    @Test
    @DisplayName("VaR 분석 - 파라미터 지정")
    void varAnalysis_WithParameters() throws Exception {
        // Given
        Map<String, Object> result = createVaRResult();
        when(tradingApiService.calculateVaR("strategy-1", 0.99, 5, "2024-01-01", "2024-06-30"))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/risk/var")
                        .param("strategyId", "strategy-1")
                        .param("confidenceLevel", "0.99")
                        .param("timeHorizon", "5")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/var-analysis"))
                .andExpect(model().attribute("strategyId", "strategy-1"))
                .andExpect(model().attribute("confidenceLevel", 0.99))
                .andExpect(model().attribute("timeHorizon", 5));
    }

    @Test
    @DisplayName("VaR 분석 - API 오류")
    void varAnalysis_ApiError() throws Exception {
        // Given
        when(tradingApiService.calculateVaR(isNull(), anyDouble(), anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/risk/var"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/var-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 상관관계 분석 ==========

    @Test
    @DisplayName("상관관계 분석 페이지 - 기본값")
    void correlationAnalysis_DefaultValues() throws Exception {
        // Given
        Map<String, Object> result = createCorrelationResult();
        when(tradingApiService.getCorrelationAnalysis(anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/risk/correlation"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/correlation-analysis"))
                .andExpect(model().attributeExists("correlationMatrix"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("상관관계 분석 - 기간 지정")
    void correlationAnalysis_WithDateRange() throws Exception {
        // Given
        Map<String, Object> result = createCorrelationResult();
        when(tradingApiService.getCorrelationAnalysis("2024-01-01", "2024-06-30")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/risk/correlation")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/correlation-analysis"))
                .andExpect(model().attribute("startDate", "2024-01-01"))
                .andExpect(model().attribute("endDate", "2024-06-30"));
    }

    @Test
    @DisplayName("상관관계 분석 - API 오류")
    void correlationAnalysis_ApiError() throws Exception {
        // Given
        when(tradingApiService.getCorrelationAnalysis(anyString(), anyString()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/risk/correlation"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/correlation-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 리스크 대시보드 ==========

    @Test
    @DisplayName("리스크 대시보드 - 성공")
    void riskDashboard_Success() throws Exception {
        // Given
        Map<String, Object> varResult = createVaRResult();
        // risk-dashboard.html은 다른 correlationMatrix 형식을 사용
        Map<String, Object> correlationResult = createCorrelationResultForDashboard();
        when(tradingApiService.calculateVaR(isNull(), eq(0.95), eq(1), anyString(), anyString()))
                .thenReturn(varResult);
        when(tradingApiService.getCorrelationAnalysis(anyString(), anyString()))
                .thenReturn(correlationResult);

        // When & Then
        mockMvc.perform(get("/trading/risk/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-dashboard"))
                .andExpect(model().attributeExists("varData"))
                .andExpect(model().attributeExists("correlationMatrix"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @DisplayName("리스크 대시보드 - API 오류")
    void riskDashboard_ApiError() throws Exception {
        // Given
        when(tradingApiService.calculateVaR(isNull(), anyDouble(), anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/risk/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createVaRResult() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> varData = new HashMap<>();
        // 기존 필드
        varData.put("var95", new BigDecimal("-500000"));
        varData.put("var99", new BigDecimal("-750000"));
        varData.put("cvar95", new BigDecimal("-600000"));
        varData.put("cvar99", new BigDecimal("-850000"));
        // 템플릿에서 사용하는 필드
        varData.put("dailyVaR", new BigDecimal("-500000"));
        varData.put("weeklyVaR", new BigDecimal("-1100000"));
        varData.put("monthlyVaR", new BigDecimal("-2200000"));
        varData.put("periodVaR", new BigDecimal("-500000"));  // var-analysis.html에서 사용
        varData.put("cvar", new BigDecimal("-600000"));
        varData.put("portfolioValue", new BigDecimal("100000000"));
        varData.put("confidenceLevel", 0.95);
        varData.put("meanPL", new BigDecimal("15000"));
        varData.put("stdPL", new BigDecimal("125000"));

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("mean", new BigDecimal("15000"));
        statistics.put("stdDev", new BigDecimal("125000"));
        statistics.put("skewness", new BigDecimal("-0.35"));
        statistics.put("kurtosis", new BigDecimal("4.2"));
        // var-analysis.html 템플릿에서 사용하는 필드
        statistics.put("meanPL", new BigDecimal("15000"));
        statistics.put("stdPL", new BigDecimal("125000"));
        statistics.put("maxDrawdown", new BigDecimal("-350000"));
        statistics.put("maxProfit", new BigDecimal("500000"));
        statistics.put("sharpeRatio", new BigDecimal("1.85"));

        result.put("varData", varData);
        result.put("statistics", statistics);
        return result;
    }

    private Map<String, Object> createCorrelationResult() {
        Map<String, Object> result = new HashMap<>();

        // 템플릿 구조에 맞게 strategies를 List<String>으로 생성
        List<String> strategies = Arrays.asList("전략 A", "전략 B", "전략 C");

        // 템플릿 구조에 맞게 correlationMatrix를 2D List로 생성
        // correlationMatrix[rowStat.index][colStat.index]
        List<List<Double>> correlationMatrix = new ArrayList<>();

        // 전략 A 행: [A-A, A-B, A-C]
        correlationMatrix.add(Arrays.asList(1.00, 0.35, -0.12));
        // 전략 B 행: [B-A, B-B, B-C]
        correlationMatrix.add(Arrays.asList(0.35, 1.00, 0.22));
        // 전략 C 행: [C-A, C-B, C-C]
        correlationMatrix.add(Arrays.asList(-0.12, 0.22, 1.00));

        result.put("correlationMatrix", correlationMatrix);
        result.put("strategies", strategies);
        return result;
    }

    // risk-dashboard.html용 correlationMatrix (다른 형식)
    private Map<String, Object> createCorrelationResultForDashboard() {
        Map<String, Object> result = new HashMap<>();

        // risk-dashboard.html은 item.strategy1, item.strategy2, item.correlation 형식 사용
        List<Map<String, Object>> correlationMatrix = new ArrayList<>();

        Map<String, Object> corr1 = new HashMap<>();
        corr1.put("strategy1", "전략 A");
        corr1.put("strategy2", "전략 A");
        corr1.put("correlation", 1.00);
        correlationMatrix.add(corr1);

        Map<String, Object> corr2 = new HashMap<>();
        corr2.put("strategy1", "전략 B");
        corr2.put("strategy2", "전략 B");
        corr2.put("correlation", 1.00);
        correlationMatrix.add(corr2);

        Map<String, Object> corr3 = new HashMap<>();
        corr3.put("strategy1", "전략 C");
        corr3.put("strategy2", "전략 C");
        corr3.put("correlation", 1.00);
        correlationMatrix.add(corr3);

        result.put("correlationMatrix", correlationMatrix);
        return result;
    }
}
