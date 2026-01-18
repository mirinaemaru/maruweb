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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HealthCheckController 통합테스트")
class HealthCheckControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        // 기본 mock 설정
    }

    @Test
    @DisplayName("Health Check 페이지 - Trading API UP")
    void page_TradingApiUp() throws Exception {
        // Given
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("version", "1.0.0");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);

        Map<String, Object> strategiesData = new HashMap<>();
        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> activeStrategy = new HashMap<>();
        activeStrategy.put("strategyId", "s1");
        activeStrategy.put("status", "ACTIVE");
        strategies.add(activeStrategy);
        strategiesData.put("items", strategies);
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        Map<String, Object> accountsData = new HashMap<>();
        List<Map<String, Object>> accounts = new ArrayList<>();
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", "account-1");
        accounts.add(account);
        accountsData.put("items", accounts);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        Map<String, Object> ordersData = new HashMap<>();
        ordersData.put("items", Collections.emptyList());
        when(tradingApiService.getOrders(anyString())).thenReturn(ordersData);

        Map<String, Object> fillsData = new HashMap<>();
        fillsData.put("items", Collections.emptyList());
        when(tradingApiService.getFills(anyString(), isNull(), isNull())).thenReturn(fillsData);

        // When & Then
        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attributeExists("tradingApiHealth"))
                .andExpect(model().attributeExists("dbHealth"))
                .andExpect(model().attributeExists("activeStrategyCount"))
                .andExpect(model().attributeExists("todayStats"))
                .andExpect(model().attributeExists("currentTime"));
    }

    @Test
    @DisplayName("Health Check 페이지 - Trading API DOWN")
    void page_TradingApiDown() throws Exception {
        // Given
        when(tradingApiService.getHealthStatus()).thenThrow(new RuntimeException("Connection refused"));
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Connection refused"));
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("Connection refused"));

        // When & Then
        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attributeExists("tradingApiHealth"))
                .andExpect(model().attributeExists("dbHealth"));
    }

    @Test
    @DisplayName("Health Check 페이지 - 전략 조회 실패")
    void page_StrategyFetchFailed() throws Exception {
        // Given
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("Strategy fetch failed"));
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("Account fetch failed"));

        // When & Then
        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attribute("activeStrategyCount", 0));
    }

    @Test
    @DisplayName("Health Check 페이지 - 활성 전략 카운트")
    void page_ActiveStrategyCount() throws Exception {
        // Given
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);

        Map<String, Object> strategiesData = new HashMap<>();
        List<Map<String, Object>> strategies = new ArrayList<>();

        Map<String, Object> s1 = new HashMap<>();
        s1.put("strategyId", "s1");
        s1.put("status", "ACTIVE");
        strategies.add(s1);

        Map<String, Object> s2 = new HashMap<>();
        s2.put("strategyId", "s2");
        s2.put("status", "ACTIVE");
        strategies.add(s2);

        Map<String, Object> s3 = new HashMap<>();
        s3.put("strategyId", "s3");
        s3.put("status", "INACTIVE");
        strategies.add(s3);

        strategiesData.put("items", strategies);
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/health-check"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/health-check"))
                .andExpect(model().attribute("activeStrategyCount", 2));
    }
}
