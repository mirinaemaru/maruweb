package com.maru.trading.controller;

import com.maru.trading.controller.TestConfig;
import com.maru.trading.service.ExcelExportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(TradingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TradingController 단위 테스트")
class TradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @MockBean
    private ExcelExportService excelExportService;

    private Map<String, Object> createMockHealthResponse() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        return health;
    }

    private Map<String, Object> createMockKillSwitchResponse() {
        Map<String, Object> killSwitch = new HashMap<>();
        killSwitch.put("status", "OFF");
        killSwitch.put("reason", "");
        killSwitch.put("triggeredAt", null);
        return killSwitch;
    }

    private Map<String, Object> createMockAccountsResponse() {
        Map<String, Object> accounts = new HashMap<>();
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "test-account-001");
        account1.put("alias", "Test Account");
        account1.put("environment", "PAPER");
        account1.put("cano", "12345678");
        account1.put("broker", "KIS");
        account1.put("status", "ACTIVE");
        account1.put("acntPrdtCd", "01");
        account1.put("createdAt", "2025-01-01T10:00:00");
        accounts.put("items", Arrays.asList(account1));
        return accounts;
    }

    private Map<String, Object> createMockStrategiesResponse() {
        Map<String, Object> strategies = new HashMap<>();
        Map<String, Object> strategy1 = new HashMap<>();
        strategy1.put("strategyId", "strategy-001");
        strategy1.put("status", "ACTIVE");
        strategies.put("items", Arrays.asList(strategy1));
        return strategies;
    }

    private Map<String, Object> createMockDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayOrders", 10);
        stats.put("todayFills", 8);
        stats.put("todayProfitLoss", 50000);
        stats.put("totalProfitLoss", 1000000);
        stats.put("winRate", 0.65);
        stats.put("recentActivities", Arrays.asList());
        stats.put("dailyStats", Arrays.asList());
        return stats;
    }

    // ==================== Dashboard Tests ====================

    @Test
    @DisplayName("대시보드 페이지 조회 - 성공")
    void dashboard_Success() throws Exception {
        when(tradingApiService.getHealthStatus()).thenReturn(createMockHealthResponse());
        when(tradingApiService.getKillSwitchStatus()).thenReturn(createMockKillSwitchResponse());
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());
        when(tradingApiService.getStrategies()).thenReturn(createMockStrategiesResponse());
        when(tradingApiService.getDashboardStats()).thenReturn(createMockDashboardStats());

        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/dashboard"))
                .andExpect(model().attributeExists("health"))
                .andExpect(model().attributeExists("killSwitch"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("systemStatus", "UP"));
    }

    @Test
    @DisplayName("대시보드 페이지 조회 - API 오류 시 에러 페이지")
    void dashboard_ApiError_ShowsErrorPage() throws Exception {
        when(tradingApiService.getHealthStatus()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/error"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("대시보드 페이지 조회 - 통계 데이터 검증")
    void dashboard_StatsDataVerification() throws Exception {
        when(tradingApiService.getHealthStatus()).thenReturn(createMockHealthResponse());
        when(tradingApiService.getKillSwitchStatus()).thenReturn(createMockKillSwitchResponse());
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());
        when(tradingApiService.getStrategies()).thenReturn(createMockStrategiesResponse());
        when(tradingApiService.getDashboardStats()).thenReturn(createMockDashboardStats());

        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/dashboard"))
                .andExpect(model().attribute("todayOrders", 10))
                .andExpect(model().attribute("todayFills", 8))
                .andExpect(model().attribute("todayProfitLoss", 50000))
                .andExpect(model().attribute("totalProfitLoss", 1000000))
                .andExpect(model().attribute("winRate", 0.65))
                .andExpect(model().attributeExists("recentActivities"))
                .andExpect(model().attributeExists("dailyStats"));
    }

    @Test
    @DisplayName("대시보드 페이지 조회 - 통계 API 실패 시 빈 통계로 페이지 로드")
    void dashboard_StatsApiFailure_LoadsPageWithEmptyStats() throws Exception {
        Map<String, Object> emptyStats = new HashMap<>();
        emptyStats.put("todayOrders", 0);
        emptyStats.put("todayFills", 0);
        emptyStats.put("todayProfitLoss", 0);
        emptyStats.put("totalProfitLoss", 0);
        emptyStats.put("winRate", 0.0);
        emptyStats.put("recentActivities", Arrays.asList());
        emptyStats.put("dailyStats", Arrays.asList());

        when(tradingApiService.getHealthStatus()).thenReturn(createMockHealthResponse());
        when(tradingApiService.getKillSwitchStatus()).thenReturn(createMockKillSwitchResponse());
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());
        when(tradingApiService.getStrategies()).thenReturn(createMockStrategiesResponse());
        when(tradingApiService.getDashboardStats()).thenReturn(emptyStats);

        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/dashboard"))
                .andExpect(model().attribute("todayOrders", 0))
                .andExpect(model().attribute("todayFills", 0))
                .andExpect(model().attribute("winRate", 0.0));
    }

    @Test
    @DisplayName("대시보드 페이지 조회 - 활성 전략 카운트 검증")
    void dashboard_ActiveStrategyCount() throws Exception {
        Map<String, Object> strategies = new HashMap<>();
        Map<String, Object> strategy1 = new HashMap<>();
        strategy1.put("strategyId", "strategy-001");
        strategy1.put("status", "ACTIVE");
        Map<String, Object> strategy2 = new HashMap<>();
        strategy2.put("strategyId", "strategy-002");
        strategy2.put("status", "INACTIVE");
        Map<String, Object> strategy3 = new HashMap<>();
        strategy3.put("strategyId", "strategy-003");
        strategy3.put("status", "ACTIVE");
        strategies.put("items", Arrays.asList(strategy1, strategy2, strategy3));

        when(tradingApiService.getHealthStatus()).thenReturn(createMockHealthResponse());
        when(tradingApiService.getKillSwitchStatus()).thenReturn(createMockKillSwitchResponse());
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());
        when(tradingApiService.getStrategies()).thenReturn(strategies);
        when(tradingApiService.getDashboardStats()).thenReturn(createMockDashboardStats());

        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/dashboard"))
                .andExpect(model().attribute("activeStrategyCount", 2L));
    }

    // ==================== Accounts Tests ====================

    @Test
    @DisplayName("계좌 목록 페이지 조회 - 성공")
    void accounts_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        mockMvc.perform(get("/trading/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/accounts"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("계좌 등록 페이지 조회")
    void newAccount_Success() throws Exception {
        mockMvc.perform(get("/trading/accounts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/account-form"));
    }

    @Test
    @DisplayName("계좌 등록 처리 - 성공")
    void createAccount_Success() throws Exception {
        Map<String, Object> createdAccount = new HashMap<>();
        createdAccount.put("accountId", "new-account");
        when(tradingApiService.createAccount(any())).thenReturn(createdAccount);

        mockMvc.perform(post("/trading/accounts")
                        .param("broker", "KIS")
                        .param("cano", "12345678")
                        .param("acntPrdtCd", "01")
                        .param("alias", "Test Account")
                        .param("environment", "SIMULATION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("계좌 상세보기 페이지 조회")
    void viewAccount_Success() throws Exception {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", "test-account-001");
        account.put("alias", "Test Account");
        account.put("environment", "PAPER");
        account.put("cano", "12345678");
        account.put("broker", "KIS");
        account.put("status", "ACTIVE");
        account.put("acntPrdtCd", "01");
        account.put("createdAt", "2025-01-01T10:00:00");
        account.put("updatedAt", "2025-01-01T10:00:00");
        when(tradingApiService.getAccount("test-account-001")).thenReturn(account);

        mockMvc.perform(get("/trading/accounts/test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/account-detail"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("계좌 삭제 처리 - 성공")
    void deleteAccount_Success() throws Exception {
        mockMvc.perform(post("/trading/accounts/test-account-001/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts"))
                .andExpect(flash().attributeExists("message"));
    }

    // ==================== Orders Tests ====================

    @Test
    @DisplayName("주문 목록 페이지 조회 - 성공")
    void orders_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        Map<String, Object> ordersResponse = new HashMap<>();
        ordersResponse.put("items", Arrays.asList());
        when(tradingApiService.getOrders(anyString())).thenReturn(ordersResponse);

        mockMvc.perform(get("/trading/orders")
                        .param("accountId", "test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/orders"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("주문 상세 페이지 조회")
    void orderDetail_Success() throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "order-001");
        order.put("status", "FILLED");
        order.put("side", "BUY");
        order.put("symbol", "005930");
        order.put("quantity", 100);
        order.put("price", 70000);
        order.put("avgPrice", 70000);
        order.put("filledQuantity", 100);
        order.put("remainingQuantity", 0);
        order.put("createdAt", "2025-06-01T10:00:00");
        order.put("updatedAt", "2025-06-01T10:00:00");
        order.put("orderType", "LIMIT");
        order.put("timeInForce", "DAY");
        order.put("accountId", "test-account-001");
        order.put("strategyId", "strategy-001");
        order.put("filledAt", "2025-06-01T10:05:00");
        order.put("cancelledAt", null);
        when(tradingApiService.getOrder("order-001")).thenReturn(order);

        mockMvc.perform(get("/trading/orders/order-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/order-detail"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    @DisplayName("주문 취소 처리 - 성공")
    void cancelOrder_Success() throws Exception {
        Map<String, Object> cancelResponse = new HashMap<>();
        cancelResponse.put("status", "CANCELLED");
        when(tradingApiService.cancelOrder("order-001")).thenReturn(cancelResponse);

        mockMvc.perform(post("/trading/orders/order-001/cancel")
                        .param("accountId", "test-account-001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/orders?accountId=test-account-001"))
                .andExpect(flash().attributeExists("message"));
    }

    // ==================== Positions Tests ====================

    @Test
    @DisplayName("포지션 목록 페이지 조회 - 성공")
    void positions_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        Map<String, Object> positionsResponse = new HashMap<>();
        positionsResponse.put("items", Arrays.asList());
        positionsResponse.put("total", 0);
        when(tradingApiService.getPositions(anyString())).thenReturn(positionsResponse);

        Map<String, Object> balanceResponse = new HashMap<>();
        balanceResponse.put("totalBalance", 10000000);
        balanceResponse.put("totalAssets", 15000000);
        balanceResponse.put("availableBalance", 8000000);
        balanceResponse.put("cashBalance", 8000000);
        balanceResponse.put("stockValue", 7000000);
        balanceResponse.put("totalProfitLoss", 500000);
        when(tradingApiService.getAccountBalance(anyString())).thenReturn(balanceResponse);

        mockMvc.perform(get("/trading/positions")
                        .param("accountId", "test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/positions"))
                .andExpect(model().attributeExists("positions"));
    }

    @Test
    @DisplayName("포지션 상세 페이지 조회")
    void positionDetail_Success() throws Exception {
        Map<String, Object> position = new HashMap<>();
        position.put("positionId", "position-001");
        position.put("symbol", "005930");
        position.put("side", "LONG");
        position.put("quantity", 100);
        position.put("avgPrice", 70000);
        position.put("avgEntryPrice", 70000);
        position.put("currentPrice", 72000);
        position.put("unrealizedPnl", 200000);
        position.put("unrealizedPnL", 200000);
        position.put("totalProfitLoss", 200000);
        position.put("marketValue", 7200000);
        position.put("costBasis", 7000000);
        position.put("accountId", "test-account-001");
        position.put("unrealizedPnLPercent", 2.86);
        position.put("strategyId", "strategy-001");
        position.put("status", "OPEN");
        position.put("entryDate", "2025-06-01T10:00:00");
        position.put("openedAt", "2025-06-01T10:00:00");
        position.put("createdAt", "2025-06-01T10:00:00");
        position.put("updatedAt", "2025-06-01T10:00:00");
        when(tradingApiService.getPosition("position-001")).thenReturn(position);

        mockMvc.perform(get("/trading/positions/position-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/position-detail"))
                .andExpect(model().attributeExists("position"));
    }

    // ==================== Kill Switch Tests ====================

    @Test
    @DisplayName("Kill Switch 페이지 조회 - 성공")
    void killSwitch_Success() throws Exception {
        when(tradingApiService.getKillSwitchStatus()).thenReturn(createMockKillSwitchResponse());
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        mockMvc.perform(get("/trading/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/kill-switch"))
                .andExpect(model().attributeExists("killSwitch"))
                .andExpect(model().attribute("killSwitchStatus", "OFF"));
    }

    @Test
    @DisplayName("Kill Switch 토글 - 성공")
    void toggleKillSwitch_Success() throws Exception {
        Map<String, Object> toggleResponse = new HashMap<>();
        toggleResponse.put("status", "ON");
        when(tradingApiService.toggleKillSwitch(anyString(), any(), any())).thenReturn(toggleResponse);

        mockMvc.perform(post("/trading/kill-switch/toggle")
                        .param("status", "ON")
                        .param("reason", "Emergency stop"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/kill-switch"))
                .andExpect(flash().attributeExists("message"));
    }

    // ==================== Fills Tests ====================

    @Test
    @DisplayName("체결 내역 페이지 조회 - 성공")
    void fills_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        Map<String, Object> fillsResponse = new HashMap<>();
        fillsResponse.put("items", Arrays.asList());
        when(tradingApiService.getFills(anyString(), any(), any())).thenReturn(fillsResponse);

        mockMvc.perform(get("/trading/fills")
                        .param("accountId", "test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/fills"))
                .andExpect(model().attributeExists("fills"));
    }

    @Test
    @DisplayName("체결 상세 페이지 조회")
    void fillDetail_Success() throws Exception {
        Map<String, Object> fill = new HashMap<>();
        fill.put("fillId", "fill-001");
        fill.put("price", 70000);
        fill.put("side", "BUY");
        fill.put("symbol", "005930");
        fill.put("quantity", 100);
        fill.put("filledAt", "2025-06-01T10:00:00");
        fill.put("totalValue", 7000000);
        fill.put("commission", 1000);
        fill.put("orderId", "order-001");
        fill.put("avgPrice", 70000);
        fill.put("accountId", "test-account-001");
        fill.put("strategyId", "strategy-001");
        fill.put("createdAt", "2025-06-01T10:00:00");
        fill.put("executedAt", "2025-06-01T10:00:00");
        when(tradingApiService.getFill("fill-001")).thenReturn(fill);

        mockMvc.perform(get("/trading/fills/fill-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/fill-detail"))
                .andExpect(model().attributeExists("fill"));
    }

    // ==================== Balances Tests ====================

    @Test
    @DisplayName("잔고 조회 페이지 - 성공")
    void balances_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());

        Map<String, Object> balanceResponse = new HashMap<>();
        balanceResponse.put("totalBalance", 10000000);
        balanceResponse.put("totalAssets", 15000000);
        balanceResponse.put("availableBalance", 8000000);
        balanceResponse.put("cashBalance", 8000000);
        balanceResponse.put("stockValue", 7000000);
        balanceResponse.put("totalProfitLoss", 500000);
        balanceResponse.put("realizedPnl", 300000);
        balanceResponse.put("unrealizedPnl", 200000);
        when(tradingApiService.getBalance(anyString())).thenReturn(balanceResponse);

        mockMvc.perform(get("/trading/balances")
                        .param("accountId", "test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/balances"))
                .andExpect(model().attributeExists("balance"));
    }
}
