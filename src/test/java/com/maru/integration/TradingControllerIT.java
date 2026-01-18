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
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TradingController 통합테스트")
class TradingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        // 기본 mock 설정
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ========== Dashboard Tests ==========

    @Test
    @DisplayName("트레이딩 대시보드 조회 - API 사용 가능")
    void dashboard_ApiAvailable_Success() throws Exception {
        // Given
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("uptime", "24h 30m");

        Map<String, Object> killSwitchStatus = new HashMap<>();
        killSwitchStatus.put("status", "OFF");

        List<Map<String, Object>> accounts = new ArrayList<>();
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", "account-1");
        accounts.add(account);
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        List<Map<String, Object>> strategies = new ArrayList<>();
        Map<String, Object> strategiesData = new HashMap<>();
        strategiesData.put("items", strategies);

        Map<String, Object> dashboardStats = new HashMap<>();
        dashboardStats.put("todayOrders", 10);
        dashboardStats.put("todayFills", 8);
        dashboardStats.put("todayProfitLoss", 50000);
        dashboardStats.put("totalProfitLoss", 1000000);
        dashboardStats.put("winRate", 65.5);

        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
        when(tradingApiService.getKillSwitchStatus()).thenReturn(killSwitchStatus);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getStrategies()).thenReturn(strategiesData);
        when(tradingApiService.getDashboardStats()).thenReturn(dashboardStats);

        // When & Then
        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/dashboard"))
                .andExpect(model().attributeExists("health"))
                .andExpect(model().attributeExists("systemStatus"))
                .andExpect(model().attributeExists("killSwitch"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attributeExists("strategies"));
    }

    @Test
    @DisplayName("트레이딩 대시보드 조회 - API 사용 불가")
    void dashboard_ApiUnavailable() throws Exception {
        // Given
        when(tradingApiService.getHealthStatus()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/error"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Account Tests ==========

    @Test
    @DisplayName("계좌 목록 조회 - 성공")
    void listAccounts_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        Map<String, Object> account = createAccountMock("account-1", "키움증권", "12345678", "테스트계좌");
        accounts.add(account);

        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/accounts"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("계좌 등록 폼 조회 - 성공")
    void newAccountForm_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trading/accounts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/account-form"));
    }

    @Test
    @DisplayName("계좌 등록 - 성공")
    void createAccount_Success() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", "new-account-1");

        when(tradingApiService.createAccount(anyMap())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/trading/accounts")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("broker", "키움증권")
                        .param("cano", "12345678")
                        .param("acntPrdtCd", "01")
                        .param("alias", "테스트 계좌")
                        .param("environment", "PAPER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("계좌 등록 - API 오류")
    void createAccount_ApiError_ShowsError() throws Exception {
        // Given
        when(tradingApiService.createAccount(anyMap())).thenThrow(new RuntimeException("계좌 등록 실패"));

        // When & Then
        mockMvc.perform(post("/trading/accounts")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("broker", "키움증권")
                        .param("cano", "12345678")
                        .param("acntPrdtCd", "01")
                        .param("alias", "테스트 계좌")
                        .param("environment", "PAPER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts/new"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("계좌 상세보기 - 성공")
    void viewAccount_Success() throws Exception {
        // Given
        Map<String, Object> account = createAccountMock("account-1", "키움증권", "12345678", "테스트계좌");

        when(tradingApiService.getAccount("account-1")).thenReturn(account);

        // When & Then
        mockMvc.perform(get("/trading/accounts/account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/account-detail"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount_Success() throws Exception {
        // Given
        doNothing().when(tradingApiService).deleteAccount("account-1");

        // When & Then
        mockMvc.perform(post("/trading/accounts/account-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("계좌 삭제 - API 오류")
    void deleteAccount_ApiError() throws Exception {
        // Given
        doThrow(new RuntimeException("삭제 실패")).when(tradingApiService).deleteAccount("account-1");

        // When & Then
        mockMvc.perform(post("/trading/accounts/account-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/accounts"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Order Tests ==========

    @Test
    @DisplayName("주문 목록 조회 - 성공")
    void listOrders_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "키움증권", "12345678", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        List<Map<String, Object>> orders = new ArrayList<>();
        orders.add(createOrderMock("order-1", "005930", "FILLED"));
        Map<String, Object> ordersData = new HashMap<>();
        ordersData.put("items", orders);

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getOrders("account-1")).thenReturn(ordersData);

        // When & Then
        mockMvc.perform(get("/trading/orders")
                        .param("accountId", "account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("주문 목록 조회 - 계좌 미선택")
    void listOrders_NoAccountSelected() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/orders"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attributeDoesNotExist("orders"));
    }

    @Test
    @DisplayName("주문 목록 조회 - 필터링")
    void listOrders_WithFilters() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());
        Map<String, Object> ordersData = new HashMap<>();
        ordersData.put("items", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getOrdersWithFilters(eq("account-1"), eq("2024-01-01"), eq("2024-01-31"), eq("FILLED"), eq("005930"), isNull()))
                .thenReturn(ordersData);

        // When & Then
        mockMvc.perform(get("/trading/orders")
                        .param("accountId", "account-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("status", "FILLED")
                        .param("symbol", "005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/orders"));
    }

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_Success() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        when(tradingApiService.cancelOrder("order-1")).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/trading/orders/order-1/cancel")
                        .param("accountId", "account-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/orders?accountId=account-1"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("주문 취소 - API 오류")
    void cancelOrder_ApiError() throws Exception {
        // Given
        when(tradingApiService.cancelOrder("order-1")).thenThrow(new RuntimeException("취소 실패"));

        // When & Then
        mockMvc.perform(post("/trading/orders/order-1/cancel")
                        .param("accountId", "account-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/orders?accountId=account-1"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Position Tests ==========

    @Test
    @DisplayName("포지션 목록 조회 - 성공")
    void listPositions_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "키움증권", "12345678", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        List<Map<String, Object>> positions = new ArrayList<>();
        positions.add(createPositionMock("position-1", "005930", 100));
        Map<String, Object> positionsData = new HashMap<>();
        positionsData.put("items", positions);
        positionsData.put("total", 1);

        Map<String, Object> balanceData = createBalanceMock();

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getPositions("account-1")).thenReturn(positionsData);
        when(tradingApiService.getAccountBalance("account-1")).thenReturn(balanceData);

        // When & Then - accountId가 없으면 첫 번째 계좌 자동 선택
        mockMvc.perform(get("/trading/positions"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/positions"))
                .andExpect(model().attributeExists("positions"))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    @DisplayName("포지션 목록 조회 - 계좌별 필터링")
    void listPositions_FilterByAccount() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "키움증권", "12345678", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);
        Map<String, Object> positionsData = new HashMap<>();
        positionsData.put("items", Collections.emptyList());
        positionsData.put("total", 0);
        Map<String, Object> balanceData = createBalanceMock();

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getPositions("account-1")).thenReturn(positionsData);
        when(tradingApiService.getAccountBalance("account-1")).thenReturn(balanceData);

        // When & Then
        mockMvc.perform(get("/trading/positions")
                        .param("accountId", "account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/positions"));
    }

    // ========== Kill Switch Tests ==========

    @Test
    @DisplayName("킬 스위치 페이지 조회 - 성공")
    void killSwitchPage_Success() throws Exception {
        // Given
        Map<String, Object> killSwitchStatus = createKillSwitchMock("OFF", null);

        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getKillSwitchStatus()).thenReturn(killSwitchStatus);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/kill-switch"))
                .andExpect(model().attributeExists("killSwitch"))
                .andExpect(model().attributeExists("killSwitchStatus"));
    }

    @Test
    @DisplayName("킬 스위치 토글 - 활성화")
    void toggleKillSwitch_Activate() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        when(tradingApiService.toggleKillSwitch(eq("ON"), anyString(), isNull())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/trading/kill-switch/toggle")
                        .param("status", "ON")
                        .param("reason", "긴급 상황"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/kill-switch"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("킬 스위치 토글 - 비활성화")
    void toggleKillSwitch_Deactivate() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        when(tradingApiService.toggleKillSwitch(eq("OFF"), isNull(), isNull())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/trading/kill-switch/toggle")
                        .param("status", "OFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/kill-switch"))
                .andExpect(flash().attributeExists("message"));
    }

    // ========== Fill Tests ==========

    @Test
    @DisplayName("체결 내역 조회 - 성공")
    void listFills_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "키움증권", "12345678", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        List<Map<String, Object>> fills = new ArrayList<>();
        fills.add(createFillMock("fill-1", "order-1", "005930"));
        Map<String, Object> fillsData = new HashMap<>();
        fillsData.put("items", fills);

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getFills("account-1", null, null)).thenReturn(fillsData);

        // When & Then
        mockMvc.perform(get("/trading/fills")
                        .param("accountId", "account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/fills"))
                .andExpect(model().attributeExists("fills"));
    }

    @Test
    @DisplayName("체결 내역 조회 - 계좌 미선택")
    void listFills_NoAccountSelected() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/fills"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/fills"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attributeDoesNotExist("fills"));
    }

    @Test
    @DisplayName("체결 내역 조회 - 필터링")
    void listFills_WithFilters() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());
        Map<String, Object> fillsData = new HashMap<>();
        fillsData.put("items", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getFillsWithFilters(eq("account-1"), eq("2024-01-01"), eq("2024-01-31"), isNull(), eq("005930")))
                .thenReturn(fillsData);

        // When & Then
        mockMvc.perform(get("/trading/fills")
                        .param("accountId", "account-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("symbol", "005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/fills"));
    }

    // ========== Balance Tests ==========

    @Test
    @DisplayName("잔고 조회 - 성공")
    void listBalances_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "키움증권", "12345678", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        Map<String, Object> balance = createBalanceMock();

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getBalance("account-1")).thenReturn(balance);

        // When & Then - accountId가 없으면 첫 번째 계좌 자동 선택
        mockMvc.perform(get("/trading/balances"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/balances"))
                .andExpect(model().attributeExists("balance"))
                .andExpect(model().attributeExists("accounts"));
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("계좌 목록 API 오류 시 에러 메시지 표시")
    void apiError_ShowsErrorMessage() throws Exception {
        // Given
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/accounts"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("apiConnected", false));
    }

    @Test
    @DisplayName("주문 목록 API 오류 시 에러 메시지 표시")
    void ordersApiError_ShowsErrorMessage() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getOrders("account-1")).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/orders")
                        .param("accountId", "account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/orders"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createAccountMock(String accountId, String broker, String cano, String alias) {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", accountId);
        account.put("broker", broker);
        account.put("cano", cano);
        account.put("alias", alias);
        account.put("acntPrdtCd", "01");
        account.put("environment", "PAPER");
        account.put("status", "ACTIVE");
        account.put("createdAt", "2024-01-01T00:00:00");
        account.put("updatedAt", "2024-01-01T00:00:00");
        return account;
    }

    private Map<String, Object> createOrderMock(String orderId, String symbol, String status) {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("symbol", symbol);
        order.put("stockCode", symbol);
        order.put("stockName", "삼성전자");
        order.put("status", status);
        order.put("side", "BUY");
        order.put("quantity", 100);
        order.put("qty", 100);
        order.put("price", new BigDecimal("70000"));
        order.put("orderType", "LIMIT");
        order.put("createdAt", LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        order.put("orderTime", LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        order.put("filledQuantity", 100);
        order.put("filledQty", 100);
        order.put("avgFillPrice", new BigDecimal("70000"));
        order.put("avgPrice", new BigDecimal("70000"));
        order.put("orderValue", new BigDecimal("7000000"));
        order.put("accountId", "account-1");
        return order;
    }

    private Map<String, Object> createPositionMock(String positionId, String symbol, int quantity) {
        Map<String, Object> position = new HashMap<>();
        position.put("positionId", positionId);
        position.put("symbol", symbol);
        position.put("stockCode", symbol);
        position.put("stockName", "삼성전자");
        position.put("quantity", quantity);
        position.put("qty", quantity);
        position.put("avgPrice", new BigDecimal("70000"));
        position.put("avgCost", new BigDecimal("70000"));
        position.put("currentPrice", new BigDecimal("72000"));
        position.put("marketValue", new BigDecimal("7200000"));
        position.put("totalValue", new BigDecimal("7200000"));
        position.put("costBasis", new BigDecimal("7000000"));
        position.put("unrealizedPnL", new BigDecimal("200000"));
        position.put("unrealizedPnl", new BigDecimal("200000"));
        position.put("realizedPnl", new BigDecimal("0"));
        position.put("unrealizedPnLPercent", 2.86);
        position.put("unrealizedPnlPercent", 2.86);
        position.put("accountId", "account-1");
        position.put("updatedAt", "2024-01-01T10:00:00");
        return position;
    }

    private Map<String, Object> createBalanceMock() {
        Map<String, Object> balance = new HashMap<>();
        balance.put("currency", "KRW");
        balance.put("available", new BigDecimal("5000000"));
        balance.put("total", new BigDecimal("10000000"));
        balance.put("totalAssets", new BigDecimal("15000000"));
        balance.put("buyingPower", new BigDecimal("5000000"));
        balance.put("cashBalance", new BigDecimal("5000000"));
        balance.put("stockValue", new BigDecimal("10000000"));
        balance.put("totalProfitLoss", new BigDecimal("500000"));
        balance.put("totalProfitLossPercent", 5.0);
        balance.put("realizedPnl", new BigDecimal("300000"));
        balance.put("unrealizedPnl", new BigDecimal("200000"));
        balance.put("totalPnl", new BigDecimal("500000"));
        balance.put("todayPnl", new BigDecimal("50000"));
        return balance;
    }

    private Map<String, Object> createKillSwitchMock(String status, String reason) {
        Map<String, Object> killSwitch = new HashMap<>();
        killSwitch.put("status", status);
        killSwitch.put("reason", reason);
        killSwitch.put("activatedAt", status.equals("ON") ? "2024-01-01T10:00:00" : null);
        killSwitch.put("activatedBy", status.equals("ON") ? "user" : null);
        return killSwitch;
    }

    private Map<String, Object> createFillMock(String fillId, String orderId, String symbol) {
        Map<String, Object> fill = new HashMap<>();
        fill.put("fillId", fillId);
        fill.put("orderId", orderId);
        fill.put("symbol", symbol);
        fill.put("stockCode", symbol);
        fill.put("stockName", "삼성전자");
        fill.put("side", "BUY");
        fill.put("quantity", 100);
        fill.put("qty", 100);
        fill.put("fillQty", 100);
        fill.put("price", new BigDecimal("70000"));
        fill.put("fillPrice", new BigDecimal("70000"));
        fill.put("fillQuantity", 100);
        fill.put("fillTimestamp", LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        fill.put("filledAt", LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        fill.put("commission", new BigDecimal("100"));
        fill.put("commissionAsset", "KRW");
        fill.put("fee", new BigDecimal("100"));
        fill.put("tax", new BigDecimal("70"));
        fill.put("tradeValue", new BigDecimal("7000000"));
        fill.put("totalAmount", new BigDecimal("7000170"));
        fill.put("accountId", "account-1");
        fill.put("brokerOrderNo", "BR001");
        return fill;
    }
}
