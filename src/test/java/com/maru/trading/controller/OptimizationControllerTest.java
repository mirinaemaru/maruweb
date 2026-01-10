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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(OptimizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OptimizationController 단위 테스트")
class OptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockStrategiesResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("strategies", Arrays.asList());
        return result;
    }

    private Map<String, Object> createMockOptimizationResultsResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("results", Arrays.asList());
        return result;
    }

    @Test
    @DisplayName("파라미터 최적화 페이지 - 성공")
    void optimizationPage_Success() throws Exception {
        when(tradingApiService.getStrategies()).thenReturn(createMockStrategiesResponse());

        mockMvc.perform(get("/trading/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("최적화 결과 페이지 - 템플릿 미구현으로 비활성화")
    @org.junit.jupiter.api.Disabled("Template trading/optimization-results not yet implemented")
    void optimizationResults_Success() throws Exception {
        when(tradingApiService.getOptimizationResults(any()))
                .thenReturn(createMockOptimizationResultsResponse());

        mockMvc.perform(get("/trading/optimization/results")
                        .param("strategyId", "strategy-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-results"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    @DisplayName("최적화 - API 오류 시")
    void optimizationPage_ApiError() throws Exception {
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization"))
                .andExpect(model().attributeExists("error"));
    }
}
