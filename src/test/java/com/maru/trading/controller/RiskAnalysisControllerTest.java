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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(RiskAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RiskAnalysisController 단위 테스트")
class RiskAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockVarResponse() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> varData = new HashMap<>();
        varData.put("dailyVaR", 100000);
        varData.put("weeklyVaR", 200000);
        varData.put("monthlyVaR", 500000);
        varData.put("periodVaR", 150000);
        varData.put("confidenceLevel", 0.95);
        varData.put("portfolioValue", 10000000);
        varData.put("cvar", 180000);
        varData.put("expectedShortfall", 180000);
        varData.put("meanPL", 50000);
        varData.put("stdPL", 30000);
        result.put("varData", varData);
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("meanPL", 50000);
        statistics.put("stdPL", 30000);
        statistics.put("skewness", 0.1);
        statistics.put("kurtosis", 3.0);
        statistics.put("stdDev", 30000);
        statistics.put("maxDrawdown", 150000);
        statistics.put("maxProfit", 200000);
        statistics.put("maxLoss", -100000);
        statistics.put("sharpeRatio", 1.5);
        statistics.put("sortinoRatio", 2.0);
        result.put("statistics", statistics);
        return result;
    }

    private Map<String, Object> createMockCorrelationResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("correlationMatrix", Arrays.asList());
        result.put("strategies", Arrays.asList());
        return result;
    }

    @Test
    @DisplayName("VaR 분석 페이지 - 성공")
    void varAnalysis_Success() throws Exception {
        when(tradingApiService.calculateVaR(any(), anyDouble(), anyInt(), any(), any()))
                .thenReturn(createMockVarResponse());

        mockMvc.perform(get("/trading/risk/var"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/var-analysis"))
                .andExpect(model().attributeExists("confidenceLevel"))
                .andExpect(model().attributeExists("timeHorizon"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @DisplayName("상관관계 분석 페이지 - 성공")
    void correlationAnalysis_Success() throws Exception {
        when(tradingApiService.getCorrelationAnalysis(any(), any()))
                .thenReturn(createMockCorrelationResponse());

        mockMvc.perform(get("/trading/risk/correlation"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/correlation-analysis"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @DisplayName("VaR 분석 - API 오류 시")
    void varAnalysis_ApiError() throws Exception {
        when(tradingApiService.calculateVaR(any(), anyDouble(), anyInt(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/risk/var"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/var-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("리스크 대시보드 - 성공")
    void riskDashboard_Success() throws Exception {
        when(tradingApiService.calculateVaR(any(), anyDouble(), anyInt(), any(), any()))
                .thenReturn(createMockVarResponse());
        when(tradingApiService.getCorrelationAnalysis(any(), any()))
                .thenReturn(createMockCorrelationResponse());

        mockMvc.perform(get("/trading/risk/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-dashboard"));
    }
}
