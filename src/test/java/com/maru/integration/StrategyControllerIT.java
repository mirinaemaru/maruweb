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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("StrategyController 통합테스트")
class StrategyControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        // 기본 mock 설정 - API health 응답
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ========== List Tests ==========

    @Test
    @DisplayName("전략 목록 조회 - 성공")
    void listStrategies_Success() throws Exception {
        // Given
        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "RSI 전략");
        strategy.put("type", "RSI");
        strategy.put("status", "ACTIVE");
        strategies.add(strategy);

        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", strategies);

        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        // When & Then
        mockMvc.perform(get("/trading/strategies"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/list"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("전략 목록 조회 - 상태 필터링")
    void listStrategies_FilterByStatus() throws Exception {
        // Given
        List<Map<String, Object>> strategies = new ArrayList<>();

        Map<String, Object> activeStrategy = new HashMap<>();
        activeStrategy.put("strategyId", "strategy-1");
        activeStrategy.put("name", "활성 전략");
        activeStrategy.put("status", "ACTIVE");
        strategies.add(activeStrategy);

        Map<String, Object> inactiveStrategy = new HashMap<>();
        inactiveStrategy.put("strategyId", "strategy-2");
        inactiveStrategy.put("name", "비활성 전략");
        inactiveStrategy.put("status", "INACTIVE");
        strategies.add(inactiveStrategy);

        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", strategies);

        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        // When & Then
        mockMvc.perform(get("/trading/strategies")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/list"))
                .andExpect(model().attribute("status", "ACTIVE"));
    }

    @Test
    @DisplayName("전략 목록 조회 - 키워드 검색")
    void listStrategies_FilterByKeyword() throws Exception {
        // Given
        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "RSI 전략");
        strategy.put("description", "RSI 지표를 활용한 전략");
        strategies.add(strategy);

        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", strategies);

        when(tradingApiService.getStrategies()).thenReturn(strategiesData);

        // When & Then
        mockMvc.perform(get("/trading/strategies")
                        .param("keyword", "RSI"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/list"))
                .andExpect(model().attribute("keyword", "RSI"));
    }

    @Test
    @DisplayName("전략 목록 조회 - API 오류 처리")
    void listStrategies_ApiError() throws Exception {
        // Given
        when(tradingApiService.getStrategies()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/strategies"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/list"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Create Tests ==========

    @Test
    @DisplayName("새 전략 폼 조회 - 성공")
    void newStrategyForm_Success() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/strategies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/new"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("전략 생성 - 성공")
    void createStrategy_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategyId", "new-strategy-1");

        when(tradingApiService.createStrategy(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/strategies/new")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "새 전략")
                        .param("type", "RSI")
                        .param("mode", "PAPER")
                        .param("rsiPeriod", "14")
                        .param("oversold", "30")
                        .param("overbought", "70"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies/new-strategy-1"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).createStrategy(anyMap());
    }

    @Test
    @DisplayName("전략 생성 - MA_CROSSOVER 타입")
    void createStrategy_MACrossover() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategyId", "ma-strategy-1");

        when(tradingApiService.createStrategy(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/strategies/new")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "MA 크로스오버 전략")
                        .param("type", "MA_CROSSOVER")
                        .param("mode", "LIVE")
                        .param("shortPeriod", "5")
                        .param("longPeriod", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("전략 생성 - BOLLINGER 타입")
    void createStrategy_Bollinger() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategyId", "bb-strategy-1");

        when(tradingApiService.createStrategy(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/strategies/new")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "볼린저 밴드 전략")
                        .param("type", "BOLLINGER")
                        .param("mode", "PAPER")
                        .param("bollingerPeriod", "20")
                        .param("stdDev", "2.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("전략 생성 - MACD 타입")
    void createStrategy_MACD() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("strategyId", "macd-strategy-1");

        when(tradingApiService.createStrategy(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/strategies/new")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "MACD 전략")
                        .param("type", "MACD")
                        .param("mode", "PAPER")
                        .param("fastPeriod", "12")
                        .param("slowPeriod", "26")
                        .param("signalPeriod", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("전략 생성 - API 오류")
    void createStrategy_ApiError() throws Exception {
        // Given
        when(tradingApiService.createStrategy(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/strategies/new")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "새 전략")
                        .param("type", "RSI")
                        .param("mode", "PAPER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies/new"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== View Tests ==========

    @Test
    @DisplayName("전략 상세 조회 - 성공")
    void viewStrategy_Success() throws Exception {
        // Given
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "RSI 전략");
        strategy.put("type", "RSI");
        strategy.put("status", "ACTIVE");
        strategy.put("accountId", "account-1");

        Map<String, Object> orders = new HashMap<>();
        orders.put("items", Collections.emptyList());

        when(tradingApiService.getStrategy("strategy-1")).thenReturn(strategy);
        when(tradingApiService.getOrders("account-1")).thenReturn(orders);

        // When & Then
        mockMvc.perform(get("/trading/strategies/strategy-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/view"))
                .andExpect(model().attributeExists("strategy"))
                .andExpect(model().attributeExists("recentOrders"));
    }

    @Test
    @DisplayName("전략 상세 조회 - 존재하지 않는 전략")
    void viewStrategy_NotFound() throws Exception {
        // Given
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "전략을 찾을 수 없습니다");

        when(tradingApiService.getStrategy("non-existent")).thenReturn(errorResponse);

        // When & Then
        mockMvc.perform(get("/trading/strategies/non-existent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Edit Tests ==========

    @Test
    @DisplayName("전략 수정 폼 조회 - 성공")
    void editStrategyForm_Success() throws Exception {
        // Given
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "RSI 전략");
        strategy.put("type", "RSI");

        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getStrategy("strategy-1")).thenReturn(strategy);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/strategies/strategy-1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/edit"))
                .andExpect(model().attributeExists("strategy"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("전략 수정 - 성공")
    void updateStrategy_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/edit")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "수정된 전략")
                        .param("type", "RSI")
                        .param("mode", "LIVE")
                        .param("rsiPeriod", "21")
                        .param("oversold", "25")
                        .param("overbought", "75"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies/strategy-1"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).updateStrategy(eq("strategy-1"), anyMap());
    }

    @Test
    @DisplayName("전략 수정 - API 오류")
    void updateStrategy_ApiError() throws Exception {
        // Given
        doThrow(new RuntimeException("API 오류")).when(tradingApiService).updateStrategy(anyString(), anyMap());

        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/edit")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "수정된 전략")
                        .param("type", "RSI")
                        .param("mode", "PAPER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies/strategy-1/edit"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Trading Config Tests ==========

    @Test
    @DisplayName("자동매매 설정 폼 조회 - 성공")
    void tradingConfigForm_Success() throws Exception {
        // Given
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "strategy-1");
        strategy.put("name", "RSI 전략");
        strategy.put("type", "RSI");

        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getStrategy("strategy-1")).thenReturn(strategy);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/strategies/strategy-1/trading"))
                .andExpect(status().isOk())
                .andExpect(view().name("strategy/trading-config"))
                .andExpect(model().attributeExists("strategy"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("자동매매 설정 저장 - 성공")
    void updateTradingConfig_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/trading")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("accountId", "account-1")
                        .param("symbol", "005930")
                        .param("assetType", "STOCK")
                        .param("stopLossType", "PERCENT")
                        .param("stopLossValue", "5.0")
                        .param("takeProfitType", "PERCENT")
                        .param("takeProfitValue", "10.0")
                        .param("positionSizeType", "FIXED")
                        .param("positionSizeValue", "1000000")
                        .param("maxPositions", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies/strategy-1"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).updateStrategy(eq("strategy-1"), anyMap());
    }

    // ========== Activate/Deactivate Tests ==========

    @Test
    @DisplayName("전략 활성화 - 성공")
    void activateStrategy_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).updateStrategyStatus("strategy-1", "ACTIVE");
    }

    @Test
    @DisplayName("전략 활성화 - API 오류")
    void activateStrategy_ApiError() throws Exception {
        // Given
        doThrow(new RuntimeException("API 오류")).when(tradingApiService).updateStrategyStatus(anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("전략 비활성화 - 성공")
    void deactivateStrategy_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).updateStrategyStatus("strategy-1", "INACTIVE");
    }

    // ========== Delete Tests ==========

    @Test
    @DisplayName("전략 삭제 - 성공")
    void deleteStrategy_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("success"));

        verify(tradingApiService).deleteStrategy("strategy-1");
    }

    @Test
    @DisplayName("전략 삭제 - API 오류")
    void deleteStrategy_ApiError() throws Exception {
        // Given
        doThrow(new RuntimeException("API 오류")).when(tradingApiService).deleteStrategy(anyString());

        // When & Then
        mockMvc.perform(post("/trading/strategies/strategy-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/strategies"))
                .andExpect(flash().attributeExists("error"));
    }
}
