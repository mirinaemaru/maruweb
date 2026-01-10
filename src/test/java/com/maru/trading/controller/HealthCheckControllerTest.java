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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(HealthCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HealthCheckController 단위 테스트")
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @Test
    @DisplayName("Health Check 페이지 - Trading API 정상")
    void page_TradingApiUp() throws Exception {
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");

        Map<String, Object> strategiesResponse = new HashMap<>();
        Map<String, Object> strategy1 = new HashMap<>();
        strategy1.put("strategyId", "strategy-001");
        strategy1.put("status", "ACTIVE");
        strategiesResponse.put("items", Arrays.asList(strategy1));

        Map<String, Object> accountsResponse = new HashMap<>();
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "test-account-001");
        accountsResponse.put("items", Arrays.asList(account1));

        Map<String, Object> ordersResponse = new HashMap<>();
        ordersResponse.put("items", Arrays.asList());

        Map<String, Object> fillsResponse = new HashMap<>();
        fillsResponse.put("items", Arrays.asList());

        when(tradingApiService.getHealthStatus()).thenReturn(healthResponse);
        when(tradingApiService.getStrategies()).thenReturn(strategiesResponse);
        when(tradingApiService.getAccounts()).thenReturn(accountsResponse);
        when(tradingApiService.getOrders(anyString())).thenReturn(ordersResponse);
        when(tradingApiService.getFills(anyString(), any(), any())).thenReturn(fillsResponse);

        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attributeExists("tradingApiHealth"))
                .andExpect(model().attributeExists("dbHealth"))
                .andExpect(model().attributeExists("activeStrategyCount"))
                .andExpect(model().attribute("activeStrategyCount", 1));
    }

    @Test
    @DisplayName("Health Check 페이지 - Trading API 장애")
    void page_TradingApiDown() throws Exception {
        when(tradingApiService.getHealthStatus()).thenThrow(new RuntimeException("Connection refused"));
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Connection refused"));
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attributeExists("tradingApiHealth"))
                .andExpect(model().attribute("activeStrategyCount", 0));
    }
}
