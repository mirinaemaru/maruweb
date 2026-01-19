package com.maru.trading.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradingApiService 단위 테스트")
class TradingApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        tradingApiService = new TradingApiService(restTemplate);
    }

    // ==================== Health Status Tests ====================

    @Test
    @DisplayName("Health 상태 조회 - 성공")
    void getHealthStatus_Success() {
        // given
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");
        healthResponse.put("components", new HashMap<>());

        when(restTemplate.exchange(
                eq("/health"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(healthResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getHealthStatus();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Health 상태 조회 - API 장애 시 DOWN 반환")
    void getHealthStatus_ApiFailure_ReturnsDown() {
        // given
        when(restTemplate.exchange(
                eq("/health"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getHealthStatus();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("DOWN");
        assertThat(result.get("error")).isEqualTo("Trading System API is unavailable");
    }

    // ==================== Account Tests ====================

    @Test
    @DisplayName("계좌 목록 조회 - 성공")
    void getAccounts_Success() {
        // given
        Map<String, Object> accountsResponse = new HashMap<>();
        accountsResponse.put("accounts", new java.util.ArrayList<>());
        accountsResponse.put("total", 0);

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(accountsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getAccounts();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("accounts");
    }

    @Test
    @DisplayName("계좌 목록 조회 - API 장애 시 예외 발생")
    void getAccounts_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getAccounts())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("계좌 상세 조회 - 성공")
    void getAccount_Success() {
        // given
        String accountId = "test-account-001";
        Map<String, Object> accountResponse = new HashMap<>();
        accountResponse.put("accountId", accountId);
        accountResponse.put("alias", "Test Account");

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(accountResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getAccount(accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("accountId")).isEqualTo(accountId);
    }

    @Test
    @DisplayName("계좌 생성 - 성공")
    void createAccount_Success() {
        // given
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("alias", "New Account");
        accountData.put("cano", "12345678");

        Map<String, Object> createdAccount = new HashMap<>();
        createdAccount.put("accountId", "new-account-001");
        createdAccount.put("alias", "New Account");

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(createdAccount, HttpStatus.CREATED));

        // when
        Map<String, Object> result = tradingApiService.createAccount(accountData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("accountId")).isEqualTo("new-account-001");
    }

    // ==================== Kill Switch Tests ====================

    @Test
    @DisplayName("Kill Switch 상태 조회 - 성공")
    void getKillSwitchStatus_Success() {
        // given
        Map<String, Object> killSwitchResponse = new HashMap<>();
        killSwitchResponse.put("status", "OFF");
        killSwitchResponse.put("reason", null);

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(killSwitchResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getKillSwitchStatus();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("OFF");
    }

    @Test
    @DisplayName("Kill Switch 상태 조회 - API 장애 시 UNKNOWN 반환")
    void getKillSwitchStatus_ApiFailure_ReturnsUnknown() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getKillSwitchStatus();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Kill Switch 토글 - 성공")
    void toggleKillSwitch_Success() {
        // given
        Map<String, Object> toggleResponse = new HashMap<>();
        toggleResponse.put("status", "ON");
        toggleResponse.put("reason", "Emergency stop");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(toggleResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", "Emergency stop", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ON");
    }

    // ==================== Strategy Tests ====================

    @Test
    @DisplayName("전략 목록 조회 - 성공")
    void getStrategies_Success() {
        // given
        Map<String, Object> strategiesResponse = new HashMap<>();
        strategiesResponse.put("strategies", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(strategiesResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getStrategies();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("strategies");
    }

    @Test
    @DisplayName("전략 상태 변경 - 성공")
    void updateStrategyStatus_Success() {
        // given
        String strategyId = "strategy-001";
        Map<String, Object> statusResponse = new HashMap<>();
        statusResponse.put("strategyId", strategyId);
        statusResponse.put("status", "ACTIVE");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId + "/status"),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(statusResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateStrategyStatus(strategyId, "ACTIVE");

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ACTIVE");
    }

    // ==================== Order Tests ====================

    @Test
    @DisplayName("주문 목록 조회 (필터 없음) - 성공")
    void getOrders_NoFilter_Success() {
        // given
        Map<String, Object> ordersResponse = new HashMap<>();
        ordersResponse.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(ordersResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrders(null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("orders");
    }

    @Test
    @DisplayName("주문 목록 조회 (계좌 필터) - 성공")
    void getOrders_WithAccountFilter_Success() {
        // given
        String accountId = "test-account";
        Map<String, Object> ordersResponse = new HashMap<>();
        ordersResponse.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(ordersResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrders(accountId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_Success() {
        // given
        String orderId = "order-001";
        Map<String, Object> cancelResponse = new HashMap<>();
        cancelResponse.put("orderId", orderId);
        cancelResponse.put("status", "CANCELLED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/cancel"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(cancelResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.cancelOrder(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("CANCELLED");
    }

    // ==================== Risk Rule Tests ====================

    @Test
    @DisplayName("리스크 룰 조회 - 성공")
    void getRiskRulesForAccount_Success() {
        // given
        String accountId = "test-account";
        Map<String, Object> rulesResponse = new HashMap<>();
        rulesResponse.put("rules", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/account/" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(rulesResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getRiskRulesForAccount(accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("rules");
    }

    @Test
    @DisplayName("전역 리스크 룰 업데이트 - 성공")
    void updateGlobalRiskRule_Success() {
        // given
        Map<String, Object> ruleData = new HashMap<>();
        ruleData.put("maxPositionValue", 5000000);
        ruleData.put("dailyLossLimit", 100000);

        Map<String, Object> updateResponse = new HashMap<>();
        updateResponse.put("riskRuleId", "global-001");
        updateResponse.put("scope", "GLOBAL");

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/global"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(updateResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateGlobalRiskRule(ruleData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("scope")).isEqualTo("GLOBAL");
    }

    // ==================== Dashboard Tests ====================

    @Test
    @DisplayName("대시보드 통계 조회 - 성공")
    void getDashboardStats_Success() {
        // given
        Map<String, Object> statsResponse = new HashMap<>();
        statsResponse.put("todayOrders", 10);
        statsResponse.put("todayFills", 8);
        statsResponse.put("todayProfitLoss", 50000);

        when(restTemplate.exchange(
                eq("/api/v1/query/dashboard/stats"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(statsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getDashboardStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("todayOrders")).isEqualTo(10);
    }

    @Test
    @DisplayName("대시보드 통계 조회 - API 장애 시 빈 통계 반환")
    void getDashboardStats_ApiFailure_ReturnsEmptyStats() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/query/dashboard/stats"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getDashboardStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("todayOrders")).isEqualTo(0);
        assertThat(result.get("todayFills")).isEqualTo(0);
    }

    // ==================== Position Tests ====================

    @Test
    @DisplayName("포지션 목록 조회 - 성공")
    void getPositions_Success() {
        // given
        String accountId = "test-account";
        Map<String, Object> positionsResponse = new HashMap<>();
        positionsResponse.put("positions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/positions?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(positionsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getPositions(accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("positions");
    }

    // ==================== Fill Tests ====================

    @Test
    @DisplayName("체결 내역 조회 - 성공")
    void getFills_Success() {
        // given
        Map<String, Object> fillsResponse = new HashMap<>();
        fillsResponse.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(fillsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("test-account", null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("fills");
    }

    // ==================== Balance Tests ====================

    @Test
    @DisplayName("계좌 잔고 조회 - 성공")
    void getBalance_Success() {
        // given
        String accountId = "test-account";
        Map<String, Object> balanceResponse = new HashMap<>();
        balanceResponse.put("totalBalance", 10000000);
        balanceResponse.put("availableBalance", 8000000);

        when(restTemplate.exchange(
                eq("/api/v1/query/balance?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(balanceResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBalance(accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("totalBalance")).isEqualTo(10000000);
    }

    // ==================== Backtest Tests ====================

    @Test
    @DisplayName("백테스트 결과 조회 - 성공")
    void getBacktestResults_Success() {
        // given
        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults(null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
    }

    // ==================== Instrument Tests ====================

    @Test
    @DisplayName("종목 목록 조회 - 성공")
    void getInstruments_Success() {
        // given
        Map<String, Object> instrumentsResponse = new HashMap<>();
        instrumentsResponse.put("items", new java.util.ArrayList<>());
        instrumentsResponse.put("total", 0);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(instrumentsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("items");
    }

    @Test
    @DisplayName("종목 상세 조회 - 성공")
    void getInstrument_Success() {
        // given
        String symbol = "005930";
        Map<String, Object> instrumentResponse = new HashMap<>();
        instrumentResponse.put("symbol", symbol);
        instrumentResponse.put("name", "삼성전자");

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/" + symbol),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(instrumentResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstrument(symbol);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("symbol")).isEqualTo(symbol);
    }

    // ==================== Strategy CRUD Tests ====================

    @Test
    @DisplayName("전략 상세 조회 - 성공")
    void getStrategy_Success() {
        // given
        String strategyId = "strategy-001";
        Map<String, Object> strategyResponse = new HashMap<>();
        strategyResponse.put("strategyId", strategyId);
        strategyResponse.put("name", "Test Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(strategyResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getStrategy(strategyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("strategyId")).isEqualTo(strategyId);
    }

    @Test
    @DisplayName("전략 생성 - 성공")
    void createStrategy_Success() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "New Strategy");
        strategyData.put("accountId", "account-001");

        Map<String, Object> createdStrategy = new HashMap<>();
        createdStrategy.put("strategyId", "new-strategy-001");
        createdStrategy.put("name", "New Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(createdStrategy, HttpStatus.CREATED));

        // when
        Map<String, Object> result = tradingApiService.createStrategy(strategyData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("strategyId")).isEqualTo("new-strategy-001");
    }

    @Test
    @DisplayName("전략 수정 - 성공")
    void updateStrategy_Success() {
        // given
        String strategyId = "strategy-001";
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Updated Strategy");

        Map<String, Object> updatedStrategy = new HashMap<>();
        updatedStrategy.put("strategyId", strategyId);
        updatedStrategy.put("name", "Updated Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(updatedStrategy, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateStrategy(strategyId, strategyData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("name")).isEqualTo("Updated Strategy");
    }

    @Test
    @DisplayName("전략 삭제 - 성공")
    void deleteStrategy_Success() {
        // given
        String strategyId = "strategy-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when & then (void 반환이므로 예외 없이 완료되면 성공)
        tradingApiService.deleteStrategy(strategyId);
    }

    // ==================== Account CRUD Tests ====================

    @Test
    @DisplayName("계좌 수정 - 성공")
    void updateAccount_Success() {
        // given
        String accountId = "account-001";
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("alias", "Updated Account");

        Map<String, Object> updatedAccount = new HashMap<>();
        updatedAccount.put("accountId", accountId);
        updatedAccount.put("alias", "Updated Account");

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(updatedAccount, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateAccount(accountId, accountData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("alias")).isEqualTo("Updated Account");
    }

    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount_Success() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when & then (void 반환이므로 예외 없이 완료되면 성공)
        tradingApiService.deleteAccount(accountId);
    }

    // ==================== Order Management Tests ====================

    @Test
    @DisplayName("주문 상세 조회 - 성공")
    void getOrder_Success() {
        // given
        String orderId = "order-001";
        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("orderId", orderId);
        orderResponse.put("status", "FILLED");

        when(restTemplate.exchange(
                eq("/api/v1/query/orders/" + orderId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrder(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("orderId")).isEqualTo(orderId);
    }

    @Test
    @DisplayName("주문 수정 - 성공")
    void modifyOrder_Success() {
        // given
        String orderId = "order-001";
        Double newPrice = 71000.0;
        Integer newQuantity = 200;

        Map<String, Object> modifiedOrder = new HashMap<>();
        modifiedOrder.put("orderId", orderId);
        modifiedOrder.put("status", "MODIFIED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(modifiedOrder, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.modifyOrder(orderId, newPrice, newQuantity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("MODIFIED");
    }

    @Test
    @DisplayName("주문 필터링 조회 - 성공")
    void getOrdersWithFilters_Success() {
        // given
        Map<String, Object> ordersResponse = new HashMap<>();
        ordersResponse.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(ordersResponse, HttpStatus.OK));

        // when (accountId, startDate, endDate, status, symbol, side)
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(
                "account-001", "2024-01-01", "2024-12-31", "PENDING", "005930", "BUY");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("orders");
    }

    // ==================== Demo Scenario Tests ====================

    @Test
    @DisplayName("데모 시나리오 목록 조회 - 성공")
    void getDemoScenarios_Success() {
        // given
        Map<String, Object> scenariosResponse = new HashMap<>();
        scenariosResponse.put("scenarios", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/demo/scenarios"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(scenariosResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getDemoScenarios();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("scenarios");
    }

    @Test
    @DisplayName("골든크로스 데모 실행 - 성공")
    void runGoldenCrossDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> executeResponse = new HashMap<>();
        executeResponse.put("success", true);
        executeResponse.put("message", "Demo scenario executed");

        when(restTemplate.exchange(
                eq("/api/v1/demo/golden-cross"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(executeResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runGoldenCrossDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    // ==================== Backtest CRUD Tests ====================

    @Test
    @DisplayName("백테스트 실행 - 성공")
    void runBacktest_Success() {
        // given
        Map<String, Object> backtestData = new HashMap<>();
        backtestData.put("strategyId", "strategy-001");
        backtestData.put("startDate", "2024-01-01");
        backtestData.put("endDate", "2024-06-30");

        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("backtestId", "backtest-001");
        backtestResponse.put("status", "RUNNING");

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runBacktest(backtestData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("백테스트 상세 조회 - 성공")
    void getBacktest_Success() {
        // given
        String backtestId = "backtest-001";
        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("backtestId", backtestId);
        backtestResponse.put("status", "COMPLETED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/" + backtestId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktest(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("backtestId")).isEqualTo(backtestId);
    }

    @Test
    @DisplayName("백테스트 삭제 - 성공")
    void deleteBacktest_Success() {
        // given
        String backtestId = "backtest-001";
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/" + backtestId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(deleteResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.deleteBacktest(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    // ==================== Optimization Tests ====================

    @Test
    @DisplayName("최적화 결과 조회 - 성공")
    void getOptimizationResults_Success() {
        // given
        String strategyId = "strategy-001";
        Map<String, Object> optimizationsResponse = new HashMap<>();
        optimizationsResponse.put("optimizations", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(optimizationsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOptimizationResults(strategyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("optimizations");
    }

    @Test
    @DisplayName("그리드 서치 최적화 실행 - 성공")
    void runGridSearch_Success() {
        // given
        Map<String, Object> optimizationData = new HashMap<>();
        optimizationData.put("strategyId", "strategy-001");
        optimizationData.put("parameters", new java.util.ArrayList<>());

        Map<String, Object> optimizationResponse = new HashMap<>();
        optimizationResponse.put("optimizationId", "opt-001");
        optimizationResponse.put("status", "RUNNING");

        when(restTemplate.exchange(
                eq("/api/v1/optimization/grid-search"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(optimizationResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runGridSearch(optimizationData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("RUNNING");
    }

    // ==================== Strategy Execution Tests ====================

    @Test
    @DisplayName("전략 실행 - 성공")
    void executeStrategy_Success() {
        // given
        String strategyId = "strategy-001";
        String symbol = "005930";
        String accountId = "account-001";

        Map<String, Object> executeResponse = new HashMap<>();
        executeResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId + "/execute?symbol=" + symbol + "&accountId=" + accountId),
                eq(HttpMethod.POST),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(executeResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.executeStrategy(strategyId, symbol, accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("데모 데이터 생성 - 성공")
    void generateDemoData_Success() {
        // given
        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/generate-data"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.generateDemoData();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    // ==================== Execution History Tests ====================

    @Test
    @DisplayName("실행 이력 조회 - 성공")
    void getExecutionHistory_Success() {
        // given
        Map<String, Object> historyResponse = new HashMap<>();
        historyResponse.put("history", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(historyResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("history");
    }

    // ==================== Performance Analysis Tests ====================

    @Test
    @DisplayName("성능 분석 조회 - 성공")
    void getPerformanceAnalysis_Success() {
        // given
        Map<String, Object> performanceResponse = new HashMap<>();
        performanceResponse.put("totalReturn", 15.5);
        performanceResponse.put("sharpeRatio", 1.2);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(performanceResponse, HttpStatus.OK));

        // when (period, startDate, endDate, strategyId)
        Map<String, Object> result = tradingApiService.getPerformanceAnalysis(
                "daily", "2024-01-01", "2024-12-31", "strategy-001");

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("totalReturn")).isEqualTo(15.5);
    }

    // ==================== Risk Rule CRUD Tests ====================

    @Test
    @DisplayName("계좌별 리스크 룰 업데이트 - 성공")
    void updateAccountRiskRule_Success() {
        // given
        String accountId = "account-001";
        Map<String, Object> ruleData = new HashMap<>();
        ruleData.put("maxPositionValue", 3000000);

        Map<String, Object> updateResponse = new HashMap<>();
        updateResponse.put("riskRuleId", "rule-001");
        updateResponse.put("accountId", accountId);

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/account/" + accountId),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(updateResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateAccountRiskRule(accountId, ruleData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("accountId")).isEqualTo(accountId);
    }

    @Test
    @DisplayName("심볼별 리스크 룰 업데이트 - 성공")
    void updateSymbolRiskRule_Success() {
        // given
        String accountId = "account-001";
        String symbol = "005930";
        Map<String, Object> ruleData = new HashMap<>();
        ruleData.put("maxPositionSize", 1000);

        Map<String, Object> updateResponse = new HashMap<>();
        updateResponse.put("riskRuleId", "rule-002");
        updateResponse.put("symbol", symbol);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(updateResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateSymbolRiskRule(accountId, symbol, ruleData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("symbol")).isEqualTo(symbol);
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("전략 생성 - API 장애 시 예외 발생")
    void createStrategy_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "New Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.createStrategy(strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 등록에 실패했습니다");
    }

    @Test
    @DisplayName("주문 취소 - API 장애 시 예외 발생")
    void cancelOrder_ApiFailure_ThrowsException() {
        // given
        String orderId = "order-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/cancel"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.cancelOrder(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 취소에 실패했습니다");
    }

    @Test
    @DisplayName("백테스트 실행 - API 장애 시 오류 맵 반환")
    void runBacktest_ApiFailure_ReturnsErrorMap() {
        // given
        Map<String, Object> backtestData = new HashMap<>();
        backtestData.put("strategyId", "strategy-001");

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runBacktest(backtestData);

        // then - runBacktest는 예외를 던지지 않고 오류 맵을 반환
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error").toString()).contains("백테스트 실행에 실패했습니다");
    }

    @Test
    @DisplayName("포지션 조회 - API 장애 시 예외 발생")
    void getPositions_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/query/positions?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getPositions(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("포지션 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("잔고 조회 - API 장애 시 예외 발생")
    void getBalance_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/query/balance?accountId=" + accountId),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getBalance(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 잔고를 가져올 수 없습니다");
    }

    // ==================== Account Balance Tests ====================

    @Test
    @DisplayName("계좌 잔액 조회 - 성공")
    void getAccountBalance_Success() {
        // given
        String accountId = "test-account";
        Map<String, Object> balanceResponse = new HashMap<>();
        balanceResponse.put("totalBalance", 15000000);
        balanceResponse.put("availableBalance", 12000000);
        balanceResponse.put("usedMargin", 3000000);

        when(restTemplate.exchange(
                eq("/api/v1/query/balance?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(balanceResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getAccountBalance(accountId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("totalBalance")).isEqualTo(15000000);
        assertThat(result.get("availableBalance")).isEqualTo(12000000);
    }

    @Test
    @DisplayName("계좌 잔액 조회 - API 장애 시 예외 발생")
    void getAccountBalance_ApiFailure_ThrowsException() {
        // given
        String accountId = "test-account";

        when(restTemplate.exchange(
                eq("/api/v1/query/balance?accountId=" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getAccountBalance(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 잔액을 가져올 수 없습니다");
    }

    // ==================== Position Detail Tests ====================

    @Test
    @DisplayName("포지션 상세 조회 - 성공")
    void getPosition_Success() {
        // given
        String positionId = "position-001";
        Map<String, Object> positionResponse = new HashMap<>();
        positionResponse.put("positionId", positionId);
        positionResponse.put("symbol", "005930");
        positionResponse.put("quantity", 100);

        when(restTemplate.exchange(
                eq("/api/v1/query/positions/" + positionId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(positionResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getPosition(positionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("positionId")).isEqualTo(positionId);
    }

    @Test
    @DisplayName("포지션 상세 조회 - API 장애 시 예외 발생")
    void getPosition_ApiFailure_ThrowsException() {
        // given
        String positionId = "position-001";

        when(restTemplate.exchange(
                eq("/api/v1/query/positions/" + positionId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getPosition(positionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("포지션 정보를 가져올 수 없습니다");
    }

    // ==================== Fill Detail Tests ====================

    @Test
    @DisplayName("체결 상세 조회 - 성공")
    void getFill_Success() {
        // given
        String fillId = "fill-001";
        Map<String, Object> fillResponse = new HashMap<>();
        fillResponse.put("fillId", fillId);
        fillResponse.put("price", 70000);
        fillResponse.put("quantity", 50);

        when(restTemplate.exchange(
                eq("/api/v1/query/fills/" + fillId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(fillResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFill(fillId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("fillId")).isEqualTo(fillId);
    }

    @Test
    @DisplayName("체결 상세 조회 - API 장애 시 예외 발생")
    void getFill_ApiFailure_ThrowsException() {
        // given
        String fillId = "fill-001";

        when(restTemplate.exchange(
                eq("/api/v1/query/fills/" + fillId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getFill(fillId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("체결 정보를 가져올 수 없습니다");
    }

    // ==================== Fills With Filters Tests ====================

    @Test
    @DisplayName("체결 내역 고급 필터링 조회 - 성공")
    void getFillsWithFilters_Success() {
        // given
        Map<String, Object> fillsResponse = new HashMap<>();
        fillsResponse.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(fillsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(
                "account-001", "2024-01-01", "2024-12-31", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("fills");
    }

    @Test
    @DisplayName("체결 내역 고급 필터링 조회 - API 장애 시 예외 발생")
    void getFillsWithFilters_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getFillsWithFilters(
                "account-001", "2024-01-01", "2024-12-31", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("체결 내역을 가져올 수 없습니다");
    }

    // ==================== Risk Rule Delete Tests ====================

    @Test
    @DisplayName("리스크 룰 삭제 - 성공")
    void deleteRiskRule_Success() {
        // given
        String ruleId = "rule-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/" + ruleId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when & then (void 반환이므로 예외 없이 완료되면 성공)
        tradingApiService.deleteRiskRule(ruleId);
    }

    @Test
    @DisplayName("리스크 룰 삭제 - API 장애 시 예외 발생")
    void deleteRiskRule_ApiFailure_ThrowsException() {
        // given
        String ruleId = "rule-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/" + ruleId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.deleteRiskRule(ruleId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("리스크 룰 삭제에 실패했습니다");
    }

    // ==================== Demo Signal Tests ====================

    @Test
    @DisplayName("데모 신호 주입 - 성공")
    void injectDemoSignal_Success() {
        // given
        Map<String, Object> signalData = new HashMap<>();
        signalData.put("type", "BUY");
        signalData.put("symbol", "005930");

        Map<String, Object> injectResponse = new HashMap<>();
        injectResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/signal"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(injectResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.injectDemoSignal(signalData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("데모 신호 주입 - API 장애 시 예외 발생")
    void injectDemoSignal_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> signalData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/signal"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.injectDemoSignal(signalData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("데모 신호 주입에 실패했습니다");
    }

    // ==================== Death Cross Demo Tests ====================

    @Test
    @DisplayName("Death Cross 데모 실행 - 성공")
    void runDeathCrossDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/death-cross"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runDeathCrossDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("Death Cross 데모 실행 - API 장애 시 예외 발생")
    void runDeathCrossDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/death-cross"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runDeathCrossDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Death Cross 데모 실행에 실패했습니다");
    }

    // ==================== RSI Oversold Demo Tests ====================

    @Test
    @DisplayName("RSI Oversold 데모 실행 - 성공")
    void runRsiOversoldDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/rsi-oversold"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runRsiOversoldDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("RSI Oversold 데모 실행 - API 장애 시 예외 발생")
    void runRsiOversoldDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/rsi-oversold"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runRsiOversoldDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("RSI Oversold 데모 실행에 실패했습니다");
    }

    // ==================== RSI Overbought Demo Tests ====================

    @Test
    @DisplayName("RSI Overbought 데모 실행 - 성공")
    void runRsiOverboughtDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/rsi-overbought"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runRsiOverboughtDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("RSI Overbought 데모 실행 - API 장애 시 예외 발생")
    void runRsiOverboughtDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/rsi-overbought"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runRsiOverboughtDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("RSI Overbought 데모 실행에 실패했습니다");
    }

    // ==================== Volatile Demo Tests ====================

    @Test
    @DisplayName("Volatile 데모 실행 - 성공")
    void runVolatileDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/volatile"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runVolatileDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("Volatile 데모 실행 - API 장애 시 예외 발생")
    void runVolatileDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/volatile"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runVolatileDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Volatile 데모 실행에 실패했습니다");
    }

    // ==================== Stable Demo Tests ====================

    @Test
    @DisplayName("Stable 데모 실행 - 성공")
    void runStableDemo_Success() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "005930");

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/stable"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runStableDemo(params);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("Stable 데모 실행 - API 장애 시 예외 발생")
    void runStableDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/stable"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runStableDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stable 데모 실행에 실패했습니다");
    }

    // ==================== Custom Demo Tests ====================

    @Test
    @DisplayName("커스텀 데모 실행 - 성공")
    void runCustomDemo_Success() {
        // given
        Map<String, Object> scenarioData = new HashMap<>();
        scenarioData.put("name", "Custom Scenario");
        scenarioData.put("signals", new java.util.ArrayList<>());

        Map<String, Object> demoResponse = new HashMap<>();
        demoResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/run"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(demoResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runCustomDemo(scenarioData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("커스텀 데모 실행 - API 장애 시 예외 발생")
    void runCustomDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> scenarioData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/run"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runCustomDemo(scenarioData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("커스텀 데모 실행에 실패했습니다");
    }

    // ==================== Account Status Update Tests ====================

    @Test
    @DisplayName("계좌 상태 업데이트 - 성공")
    void updateAccountStatus_Success() {
        // given
        String accountId = "account-001";
        String status = "ACTIVE";

        Map<String, Object> statusResponse = new HashMap<>();
        statusResponse.put("accountId", accountId);
        statusResponse.put("status", status);

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId + "/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(statusResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateAccountStatus(accountId, status);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo(status);
    }

    @Test
    @DisplayName("계좌 상태 업데이트 - API 장애 시 예외 발생")
    void updateAccountStatus_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId + "/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateAccountStatus(accountId, "ACTIVE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 상태 업데이트에 실패했습니다");
    }

    // ==================== Backtest Detail Tests ====================

    @Test
    @DisplayName("백테스트 상세 조회 - 성공")
    void getBacktestDetail_Success() {
        // given
        Long backtestId = 1L;
        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("id", backtestId);
        backtestResponse.put("strategyId", "strategy-001");
        backtestResponse.put("totalReturn", 15.5);

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests/" + backtestId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestDetail(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(backtestId);
    }

    @Test
    @DisplayName("백테스트 상세 조회 - API 장애 시 빈 결과 반환")
    void getBacktestDetail_ApiFailure_ReturnsEmptyDetail() {
        // given
        Long backtestId = 1L;

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests/" + backtestId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getBacktestDetail(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(backtestId);
        assertThat(result).containsKey("trades");
    }

    // ==================== Walk-Forward Analysis Tests ====================

    @Test
    @DisplayName("Walk-Forward 분석 실행 - 성공")
    void runWalkForwardAnalysis_Success() {
        // given
        Map<String, Object> request = new HashMap<>();
        request.put("strategyId", "strategy-001");
        request.put("periods", 10);

        Map<String, Object> analysisResponse = new HashMap<>();
        analysisResponse.put("success", true);
        analysisResponse.put("results", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/walk-forward"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(analysisResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runWalkForwardAnalysis(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("Walk-Forward 분석 실행 - API 장애 시 오류 맵 반환")
    void runWalkForwardAnalysis_ApiFailure_ReturnsErrorMap() {
        // given
        Map<String, Object> request = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/walk-forward"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runWalkForwardAnalysis(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Portfolio Backtest Tests ====================

    @Test
    @DisplayName("포트폴리오 백테스트 실행 - 성공")
    void runPortfolioBacktest_Success() {
        // given
        Map<String, Object> request = new HashMap<>();
        request.put("strategies", new java.util.ArrayList<>());

        Map<String, Object> portfolioResponse = new HashMap<>();
        portfolioResponse.put("success", true);
        portfolioResponse.put("results", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/portfolio"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(portfolioResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runPortfolioBacktest(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("포트폴리오 백테스트 실행 - API 장애 시 오류 맵 반환")
    void runPortfolioBacktest_ApiFailure_ReturnsErrorMap() {
        // given
        Map<String, Object> request = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/portfolio"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runPortfolioBacktest(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Backtest Trades Tests ====================

    @Test
    @DisplayName("백테스트 거래 조회 - 성공")
    void getBacktestTrades_Success() {
        // given
        Long backtestId = 1L;
        Map<String, Object> tradesResponse = new HashMap<>();
        tradesResponse.put("trades", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/" + backtestId + "/trades"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(tradesResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestTrades(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("trades");
    }

    @Test
    @DisplayName("백테스트 거래 조회 - API 장애 시 오류 맵 반환")
    void getBacktestTrades_ApiFailure_ReturnsErrorMap() {
        // given
        Long backtestId = 1L;

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/" + backtestId + "/trades"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getBacktestTrades(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
        assertThat(result).containsKey("trades");
    }

    // ==================== Strategy Statistics Tests ====================

    @Test
    @DisplayName("전략별 통계 조회 - 성공")
    void getStrategyStatistics_Success() {
        // given
        Map<String, Object> statsResponse = new HashMap<>();
        statsResponse.put("strategies", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(statsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getStrategyStatistics("2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("strategies");
    }

    @Test
    @DisplayName("전략별 통계 조회 - API 장애 시 오류 맵 반환")
    void getStrategyStatistics_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getStrategyStatistics("2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Genetic Algorithm Tests ====================

    @Test
    @DisplayName("유전 알고리즘 실행 - 성공")
    void runGeneticAlgorithm_Success() {
        // given
        Map<String, Object> request = new HashMap<>();
        request.put("strategyId", "strategy-001");
        request.put("generations", 100);

        Map<String, Object> gaResponse = new HashMap<>();
        gaResponse.put("success", true);
        gaResponse.put("bestParameters", new HashMap<>());

        when(restTemplate.exchange(
                eq("/api/v1/optimization/genetic-algorithm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(gaResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runGeneticAlgorithm(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("유전 알고리즘 실행 - API 장애 시 오류 맵 반환")
    void runGeneticAlgorithm_ApiFailure_ReturnsErrorMap() {
        // given
        Map<String, Object> request = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/optimization/genetic-algorithm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runGeneticAlgorithm(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result).containsKey("error");
    }

    // ==================== VaR Calculation Tests ====================

    @Test
    @DisplayName("VaR 계산 - 성공")
    void calculateVaR_Success() {
        // given
        Map<String, Object> varResponse = new HashMap<>();
        varResponse.put("var", 150000);
        varResponse.put("confidenceLevel", 0.95);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(varResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.calculateVaR(
                "strategy-001", 0.95, 1, "2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("var")).isEqualTo(150000);
    }

    @Test
    @DisplayName("VaR 계산 - API 장애 시 오류 맵 반환")
    void calculateVaR_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.calculateVaR(
                "strategy-001", 0.95, 1, "2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Correlation Analysis Tests ====================

    @Test
    @DisplayName("상관관계 분석 - 성공")
    void getCorrelationAnalysis_Success() {
        // given
        Map<String, Object> correlationResponse = new HashMap<>();
        correlationResponse.put("correlations", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(correlationResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getCorrelationAnalysis("2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("correlations");
    }

    @Test
    @DisplayName("상관관계 분석 - API 장애 시 오류 맵 반환")
    void getCorrelationAnalysis_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getCorrelationAnalysis("2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Instrument Status Update Tests ====================

    @Test
    @DisplayName("종목 상태 업데이트 - 성공")
    void updateInstrumentStatus_Success() {
        // given
        String symbol = "005930";
        Map<String, Object> statusResponse = new HashMap<>();
        statusResponse.put("symbol", symbol);
        statusResponse.put("status", "ACTIVE");
        statusResponse.put("tradable", true);

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/" + symbol + "/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(statusResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus(symbol, "ACTIVE", true, false);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("symbol")).isEqualTo(symbol);
    }

    @Test
    @DisplayName("종목 상태 업데이트 - API 장애 시 예외 발생")
    void updateInstrumentStatus_ApiFailure_ThrowsException() {
        // given
        String symbol = "005930";

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/" + symbol + "/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateInstrumentStatus(symbol, "ACTIVE", true, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("종목 상태 업데이트에 실패했습니다");
    }

    // ==================== List Backtests Tests ====================

    @Test
    @DisplayName("백테스트 목록 조회 - 성공 (List 응답)")
    void listBacktests_Success_ListResponse() {
        // given
        java.util.List<Map<String, Object>> backtestList = new java.util.ArrayList<>();
        Map<String, Object> backtest = new HashMap<>();
        backtest.put("id", 1L);
        backtestList.add(backtest);

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestList, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
    }

    @Test
    @DisplayName("백테스트 목록 조회 - 성공 (Map 응답)")
    void listBacktests_Success_MapResponse() {
        // given
        Map<String, Object> backtestsResponse = new HashMap<>();
        backtestsResponse.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestsResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("백테스트 목록 조회 - API 장애 시 빈 목록 반환")
    void listBacktests_ApiFailure_ReturnsEmptyList() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
        assertThat(result).containsKey("error");
    }

    // ==================== MA Crossover Optimization Tests ====================

    @Test
    @DisplayName("MA 크로스오버 최적화 데모 - 성공")
    void runMACrossoverOptimization_Success() {
        // given
        Map<String, Object> optimizationResponse = new HashMap<>();
        optimizationResponse.put("success", true);
        optimizationResponse.put("bestParams", new HashMap<>());

        when(restTemplate.exchange(
                eq("/api/v1/demo/optimization/ma-crossover"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(optimizationResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runMACrossoverOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("MA 크로스오버 최적화 데모 - API 장애 시 오류 맵 반환")
    void runMACrossoverOptimization_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/optimization/ma-crossover"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runMACrossoverOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== RSI Optimization Tests ====================

    @Test
    @DisplayName("RSI 최적화 데모 - 성공")
    void runRSIOptimization_Success() {
        // given
        Map<String, Object> optimizationResponse = new HashMap<>();
        optimizationResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/optimization/rsi"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(optimizationResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runRSIOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("RSI 최적화 데모 - API 장애 시 오류 맵 반환")
    void runRSIOptimization_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/optimization/rsi"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runRSIOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Random Search Optimization Tests ====================

    @Test
    @DisplayName("랜덤 서치 최적화 데모 - 성공")
    void runRandomSearchOptimization_Success() {
        // given
        Map<String, Object> optimizationResponse = new HashMap<>();
        optimizationResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/random-search"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(optimizationResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runRandomSearchOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("랜덤 서치 최적화 데모 - API 장애 시 오류 맵 반환")
    void runRandomSearchOptimization_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/advanced/random-search"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runRandomSearchOptimization();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== MA Crossover Backtest Tests ====================

    @Test
    @DisplayName("MA 크로스오버 백테스트 데모 - 성공")
    void runMACrossoverBacktest_Success() {
        // given
        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("success", true);
        backtestResponse.put("totalReturn", 12.5);

        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/ma-crossover"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runMACrossoverBacktest();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("MA 크로스오버 백테스트 데모 - API 장애 시 오류 맵 반환")
    void runMACrossoverBacktest_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/ma-crossover"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runMACrossoverBacktest();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== RSI Backtest Tests ====================

    @Test
    @DisplayName("RSI 백테스트 데모 - 성공")
    void runRSIBacktest_Success() {
        // given
        Map<String, Object> backtestResponse = new HashMap<>();
        backtestResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/rsi"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runRSIBacktest();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("RSI 백테스트 데모 - API 장애 시 오류 맵 반환")
    void runRSIBacktest_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/rsi"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runRSIBacktest();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Backtest Comparison Tests ====================

    @Test
    @DisplayName("백테스트 비교 데모 - 성공")
    void runBacktestComparison_Success() {
        // given
        Map<String, Object> comparisonResponse = new HashMap<>();
        comparisonResponse.put("success", true);
        comparisonResponse.put("comparisons", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/compare"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(comparisonResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.runBacktestComparison();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("백테스트 비교 데모 - API 장애 시 오류 맵 반환")
    void runBacktestComparison_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/compare"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runBacktestComparison();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Clear Demo Data Tests ====================

    @Test
    @DisplayName("데모 데이터 삭제 - 성공")
    void clearDemoData_Success() {
        // given
        Map<String, Object> clearResponse = new HashMap<>();
        clearResponse.put("success", true);

        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/clear"),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(clearResponse, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.clearDemoData();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("데모 데이터 삭제 - API 장애 시 오류 맵 반환")
    void clearDemoData_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/clear"),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.clearDemoData();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    // ==================== Additional Error Handling Tests ====================

    @Test
    @DisplayName("전략 목록 조회 - API 장애 시 예외 발생")
    void getStrategies_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getStrategies())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("전략 상세 조회 - API 장애 시 예외 발생")
    void getStrategy_ApiFailure_ThrowsException() {
        // given
        String strategyId = "strategy-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getStrategy(strategyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 정보를 가져올 수 없습니다");
    }

    @Test
    @DisplayName("전략 수정 - API 장애 시 예외 발생")
    void updateStrategy_ApiFailure_ThrowsException() {
        // given
        String strategyId = "strategy-001";
        Map<String, Object> strategyData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy(strategyId, strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System에 연결할 수 없습니다");
    }

    @Test
    @DisplayName("전략 삭제 - API 장애 시 예외 발생")
    void deleteStrategy_ApiFailure_ThrowsException() {
        // given
        String strategyId = "strategy-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.deleteStrategy(strategyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 삭제에 실패했습니다");
    }

    @Test
    @DisplayName("전략 상태 변경 - API 장애 시 예외 발생")
    void updateStrategyStatus_ApiFailure_ThrowsException() {
        // given
        String strategyId = "strategy-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/" + strategyId + "/status"),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategyStatus(strategyId, "ACTIVE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 상태 변경에 실패했습니다");
    }

    @Test
    @DisplayName("계좌 상세 조회 - API 장애 시 예외 발생")
    void getAccount_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getAccount(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 정보를 가져올 수 없습니다");
    }

    @Test
    @DisplayName("계좌 생성 - API 장애 시 예외 발생")
    void createAccount_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> accountData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.createAccount(accountData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 등록에 실패했습니다");
    }

    @Test
    @DisplayName("계좌 수정 - API 장애 시 예외 발생")
    void updateAccount_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";
        Map<String, Object> accountData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateAccount(accountId, accountData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 수정에 실패했습니다");
    }

    @Test
    @DisplayName("계좌 삭제 - API 장애 시 예외 발생")
    void deleteAccount_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/accounts/" + accountId),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.deleteAccount(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 삭제에 실패했습니다");
    }

    @Test
    @DisplayName("Kill Switch 토글 - API 장애 시 예외 발생")
    void toggleKillSwitch_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.toggleKillSwitch("ON", "Emergency", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kill Switch 변경에 실패했습니다");
    }

    @Test
    @DisplayName("주문 상세 조회 - API 장애 시 예외 발생")
    void getOrder_ApiFailure_ThrowsException() {
        // given
        String orderId = "order-001";

        when(restTemplate.exchange(
                eq("/api/v1/query/orders/" + orderId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getOrder(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 정보를 가져올 수 없습니다");
    }

    @Test
    @DisplayName("주문 목록 조회 - API 장애 시 예외 발생")
    void getOrders_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/query/orders"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getOrders(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("주문 필터링 조회 - API 장애 시 예외 발생")
    void getOrdersWithFilters_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getOrdersWithFilters(
                "account-001", null, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("주문 수정 - API 장애 시 예외 발생")
    void modifyOrder_ApiFailure_ThrowsException() {
        // given
        String orderId = "order-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.modifyOrder(orderId, 71000.0, 200))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 수정에 실패했습니다");
    }

    @Test
    @DisplayName("리스크 룰 조회 - API 장애 시 예외 발생")
    void getRiskRulesForAccount_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/account/" + accountId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getRiskRulesForAccount(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("리스크 룰을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("전역 리스크 룰 업데이트 - API 장애 시 예외 발생")
    void updateGlobalRiskRule_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> ruleData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/global"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateGlobalRiskRule(ruleData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전역 리스크 룰 업데이트에 실패했습니다");
    }

    @Test
    @DisplayName("계좌별 리스크 룰 업데이트 - API 장애 시 예외 발생")
    void updateAccountRiskRule_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";
        Map<String, Object> ruleData = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/admin/risk-rules/account/" + accountId),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateAccountRiskRule(accountId, ruleData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌 리스크 룰 업데이트에 실패했습니다");
    }

    @Test
    @DisplayName("종목별 리스크 룰 업데이트 - API 장애 시 예외 발생")
    void updateSymbolRiskRule_ApiFailure_ThrowsException() {
        // given
        String accountId = "account-001";
        String symbol = "005930";
        Map<String, Object> ruleData = new HashMap<>();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateSymbolRiskRule(accountId, symbol, ruleData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("종목 리스크 룰 업데이트에 실패했습니다");
    }

    @Test
    @DisplayName("데모 시나리오 목록 조회 - API 장애 시 예외 발생")
    void getDemoScenarios_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/scenarios"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getDemoScenarios())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("데모 시나리오 목록을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("골든크로스 데모 실행 - API 장애 시 예외 발생")
    void runGoldenCrossDemo_ApiFailure_ThrowsException() {
        // given
        Map<String, Object> params = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/demo/golden-cross"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.runGoldenCrossDemo(params))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Golden Cross 데모 실행에 실패했습니다");
    }

    @Test
    @DisplayName("전략 실행 - API 장애 시 예외 발생")
    void executeStrategy_ApiFailure_ThrowsException() {
        // given
        String strategyId = "strategy-001";
        String symbol = "005930";
        String accountId = "account-001";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.executeStrategy(strategyId, symbol, accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("전략 수동 실행에 실패했습니다");
    }

    @Test
    @DisplayName("체결 내역 조회 - API 장애 시 예외 발생")
    void getFills_ApiFailure_ThrowsException() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> tradingApiService.getFills("account-001", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("체결 내역을 가져올 수 없습니다");
    }

    @Test
    @DisplayName("그리드 서치 실행 - API 장애 시 오류 맵 반환")
    void runGridSearch_ApiFailure_ReturnsErrorMap() {
        // given
        Map<String, Object> request = new HashMap<>();

        when(restTemplate.exchange(
                eq("/api/v1/optimization/grid-search"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.runGridSearch(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("최적화 결과 조회 - API 장애 시 오류 맵 반환")
    void getOptimizationResults_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getOptimizationResults("strategy-001");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("성능 분석 조회 - API 장애 시 오류 맵 반환")
    void getPerformanceAnalysis_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getPerformanceAnalysis("daily", "2024-01-01", "2024-12-31", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("종목 목록 조회 - API 장애 시 오류 맵 반환")
    void getInstruments_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("items");
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("종목 상세 조회 - API 장애 시 오류 맵 반환")
    void getInstrument_ApiFailure_ReturnsErrorMap() {
        // given
        String symbol = "005930";

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/" + symbol),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getInstrument(symbol);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("백테스트 상세 조회(Admin) - API 장애 시 오류 맵 반환")
    void getBacktest_ApiFailure_ReturnsErrorMap() {
        // given
        String backtestId = "backtest-001";

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/" + backtestId),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getBacktest(backtestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("데모 데이터 생성 - API 장애 시 오류 맵 반환")
    void generateDemoData_ApiFailure_ReturnsErrorMap() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/demo/backtest/generate-data"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.generateDemoData();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("error");
    }

    @Test
    @DisplayName("백테스트 결과 조회 - API 장애 시 빈 목록 반환")
    void getBacktestResults_ApiFailure_ReturnsEmptyList() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults(null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
    }

    @Test
    @DisplayName("실행 이력 조회 - API 장애 시 빈 결과 반환")
    void getExecutionHistory_ApiFailure_ReturnsEmptyResult() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("executions");
        assertThat(result.get("totalExecutions")).isEqualTo(0);
    }

    // ==================== Branch Coverage Tests ====================

    @Test
    @DisplayName("toggleKillSwitch - reason이 null일 때")
    void toggleKillSwitch_WithNullReason() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ON");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", null, "account-001");

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ON");
    }

    @Test
    @DisplayName("toggleKillSwitch - reason이 빈 문자열일 때")
    void toggleKillSwitch_WithEmptyReason() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ON");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", "", "account-001");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("toggleKillSwitch - accountId가 null일 때")
    void toggleKillSwitch_WithNullAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ON");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", "Emergency", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("toggleKillSwitch - accountId가 빈 문자열일 때")
    void toggleKillSwitch_WithEmptyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ON");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", "Emergency", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("toggleKillSwitch - 모든 파라미터가 있을 때")
    void toggleKillSwitch_WithAllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ON");

        when(restTemplate.exchange(
                eq("/api/v1/admin/kill-switch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.toggleKillSwitch("ON", "Emergency Stop", "account-001");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrders - accountId가 null일 때")
    void getOrders_WithNullAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrders(null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrders - accountId가 빈 문자열일 때")
    void getOrders_WithEmptyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrders("");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrders - accountId가 있을 때")
    void getOrders_WithAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?accountId=account-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrders("account-001");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - 모든 파라미터가 null일 때")
    void getOrdersWithFilters_AllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, null, null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - accountId만 있을 때")
    void getOrdersWithFilters_OnlyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?accountId=account-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters("account-001", null, null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - startDate만 있을 때")
    void getOrdersWithFilters_OnlyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, "2024-01-01", null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - accountId와 startDate가 있을 때")
    void getOrdersWithFilters_AccountIdAndStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?accountId=account-001&startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters("account-001", "2024-01-01", null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - endDate만 있을 때")
    void getOrdersWithFilters_OnlyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?endDate=2024-12-31"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, null, "2024-12-31", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - status만 있을 때")
    void getOrdersWithFilters_OnlyStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?status=FILLED"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, null, null, "FILLED", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - symbol만 있을 때")
    void getOrdersWithFilters_OnlySymbol() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?symbol=005930"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, null, null, null, "005930", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - side만 있을 때")
    void getOrdersWithFilters_OnlySide() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?side=BUY"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters(null, null, null, null, null, "BUY");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - 모든 파라미터가 있을 때")
    void getOrdersWithFilters_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?accountId=account-001&startDate=2024-01-01&endDate=2024-12-31&status=FILLED&symbol=005930&side=BUY"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters("account-001", "2024-01-01", "2024-12-31", "FILLED", "005930", "BUY");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrdersWithFilters - 빈 문자열 파라미터들")
    void getOrdersWithFilters_EmptyStringParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/orders?"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOrdersWithFilters("", "", "", "", "", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - 모든 파라미터가 null일 때")
    void getFills_AllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills(null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - accountId만 있을 때")
    void getFills_OnlyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?accountId=account-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("account-001", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - orderId만 있을 때")
    void getFills_OnlyOrderId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?orderId=order-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills(null, "order-001", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - symbol만 있을 때")
    void getFills_OnlySymbol() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?symbol=005930"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills(null, null, "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - accountId와 orderId가 있을 때")
    void getFills_AccountIdAndOrderId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?accountId=account-001&orderId=order-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("account-001", "order-001", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - 모든 파라미터가 있을 때")
    void getFills_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?accountId=account-001&orderId=order-001&symbol=005930"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("account-001", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - 모든 파라미터가 null일 때")
    void getFillsWithFilters_AllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(null, null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - startDate만 있을 때")
    void getFillsWithFilters_OnlyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(null, "2024-01-01", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - endDate만 있을 때")
    void getFillsWithFilters_OnlyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?endDate=2024-12-31"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(null, null, "2024-12-31", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - accountId와 startDate가 있을 때")
    void getFillsWithFilters_AccountIdAndStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?accountId=account-001&startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "2024-01-01", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - orderId만 있을 때")
    void getFillsWithFilters_OnlyOrderId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?orderId=order-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(null, null, null, "order-001", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - 모든 파라미터가 있을 때")
    void getFillsWithFilters_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/fills?accountId=account-001&startDate=2024-01-01&endDate=2024-12-31&orderId=order-001&symbol=005930"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "2024-01-01", "2024-12-31", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("modifyOrder - newPrice가 null일 때")
    void modifyOrder_WithNullNewPrice() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", "order-001");
        response.put("status", "MODIFIED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.modifyOrder("order-001", null, 100);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("modifyOrder - newQuantity가 null일 때")
    void modifyOrder_WithNullNewQuantity() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", "order-001");
        response.put("status", "MODIFIED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.modifyOrder("order-001", 50000.0, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("modifyOrder - 모두 null일 때")
    void modifyOrder_WithAllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", "order-001");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.modifyOrder("order-001", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("modifyOrder - 모든 값이 있을 때")
    void modifyOrder_WithAllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", "order-001");
        response.put("status", "MODIFIED");

        when(restTemplate.exchange(
                eq("/api/v1/admin/orders/modify"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.modifyOrder("order-001", 50000.0, 100);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - strategyId만 있을 때")
    void getBacktestResults_OnlyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests?strategyId=strategy-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults("strategy-001", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - startDate만 있을 때")
    void getBacktestResults_OnlyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests?startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults(null, "2024-01-01", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - endDate만 있을 때")
    void getBacktestResults_OnlyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests?endDate=2024-12-31"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults(null, null, "2024-12-31");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - 모든 파라미터가 있을 때")
    void getBacktestResults_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/backtests?strategyId=strategy-001&startDate=2024-01-01&endDate=2024-12-31"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults("strategy-001", "2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - strategyId만 있을 때")
    void getExecutionHistory_OnlyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/executions?strategyId=strategy-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("strategy-001", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - startDate만 있을 때")
    void getExecutionHistory_OnlyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/executions?startDate=2024-01-01"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory(null, "2024-01-01", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - endDate만 있을 때")
    void getExecutionHistory_OnlyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/executions?endDate=2024-12-31"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory(null, null, "2024-12-31", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - status만 있을 때")
    void getExecutionHistory_OnlyStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/executions?status=SUCCESS"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory(null, null, null, "SUCCESS");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - 모든 파라미터가 있을 때")
    void getExecutionHistory_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/query/executions?strategyId=strategy-001&startDate=2024-01-01&endDate=2024-12-31&status=SUCCESS"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("strategy-001", "2024-01-01", "2024-12-31", "SUCCESS");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getPerformanceAnalysis - strategyId가 null일 때")
    void getPerformanceAnalysis_WithNullStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("performance", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getPerformanceAnalysis("daily", "2024-01-01", "2024-12-31", null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getPerformanceAnalysis - strategyId가 빈 문자열일 때")
    void getPerformanceAnalysis_WithEmptyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("performance", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getPerformanceAnalysis("daily", "2024-01-01", "2024-12-31", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getPerformanceAnalysis - strategyId가 있을 때")
    void getPerformanceAnalysis_WithStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("performance", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getPerformanceAnalysis("daily", "2024-01-01", "2024-12-31", "strategy-001");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOptimizationResults - strategyId가 null일 때")
    void getOptimizationResults_WithNullStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("results", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/optimization/results"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOptimizationResults(null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOptimizationResults - strategyId가 빈 문자열일 때")
    void getOptimizationResults_WithEmptyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("results", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/optimization/results"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOptimizationResults("");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOptimizationResults - strategyId가 있을 때")
    void getOptimizationResults_WithStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("results", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/optimization/results?strategyId=strategy-001"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getOptimizationResults("strategy-001");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - 모든 파라미터가 null일 때")
    void getInstruments_AllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - market만 있을 때")
    void getInstruments_OnlyMarket() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments?market=KOSPI"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("KOSPI", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - status만 있을 때")
    void getInstruments_OnlyStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments?status=ACTIVE"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, "ACTIVE", null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - tradable만 있을 때")
    void getInstruments_OnlyTradable() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments?tradable=true"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, null, true, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - search만 있을 때")
    void getInstruments_OnlySearch() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments?search=삼성"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments(null, null, null, "삼성");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - 모든 파라미터가 있을 때")
    void getInstruments_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments?market=KOSPI&status=ACTIVE&tradable=true&search=삼성"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("KOSPI", "ACTIVE", true, "삼성");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - 빈 문자열 파라미터들")
    void getInstruments_EmptyStringParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("", "", null, "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateInstrumentStatus - status가 null일 때")
    void updateInstrumentStatus_WithNullStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", "005930");

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/005930/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus("005930", null, true, false);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateInstrumentStatus - tradable이 null일 때")
    void updateInstrumentStatus_WithNullTradable() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", "005930");

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/005930/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus("005930", "ACTIVE", null, false);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateInstrumentStatus - halted가 null일 때")
    void updateInstrumentStatus_WithNullHalted() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", "005930");

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/005930/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus("005930", "ACTIVE", true, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateInstrumentStatus - 모두 null일 때")
    void updateInstrumentStatus_AllNull() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", "005930");

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/005930/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus("005930", null, null, null);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateInstrumentStatus - 모든 값이 있을 때")
    void updateInstrumentStatus_AllParams() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("symbol", "005930");
        response.put("status", "ACTIVE");
        response.put("tradable", true);
        response.put("halted", false);

        when(restTemplate.exchange(
                eq("/api/v1/admin/instruments/005930/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.updateInstrumentStatus("005930", "ACTIVE", true, false);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("listBacktests - List 응답일 때")
    void listBacktests_ListResponse() {
        // given
        java.util.List<Map<String, Object>> backtestList = new java.util.ArrayList<>();
        Map<String, Object> backtest = new HashMap<>();
        backtest.put("id", 1);
        backtestList.add(backtest);

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(backtestList, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
    }

    @Test
    @DisplayName("listBacktests - Map 응답일 때")
    void listBacktests_MapResponse() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());
        response.put("total", 0);

        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("backtests");
    }

    @Test
    @DisplayName("listBacktests - null 응답일 때")
    void listBacktests_NullResponse() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.listBacktests();

        // then
        assertThat(result).isNotNull();
    }

    // ==================== 추가 분기 커버리지 테스트 (Additional Branch Coverage) ====================

    @Test
    @DisplayName("updateStrategy - HttpServerErrorException 발생 시")
    void updateStrategy_HttpServerError() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                "{\"message\":\"Internal server error\"}".getBytes(), null));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 서버 오류");
    }

    @Test
    @DisplayName("updateStrategy - HttpClientErrorException 발생 시")
    void updateStrategy_HttpClientError() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request",
                "{\"detail\":\"Invalid data\"}".getBytes(), null));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 요청 오류");
    }

    @Test
    @DisplayName("updateStrategy - HttpServerErrorException with null body")
    void updateStrategy_HttpServerError_NullBody() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 서버 오류");
    }

    @Test
    @DisplayName("updateStrategy - HttpClientErrorException with empty body")
    void updateStrategy_HttpClientError_EmptyBody() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request",
                "".getBytes(), null));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 요청 오류");
    }

    @Test
    @DisplayName("updateStrategy - HttpServerErrorException with long error body")
    void updateStrategy_HttpServerError_LongBody() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        // 100자 이상의 긴 에러 메시지 생성
        String longErrorBody = "A".repeat(150);
        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                longErrorBody.getBytes(), null));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 서버 오류");
    }

    @Test
    @DisplayName("updateStrategy - HttpClientErrorException with message containing special chars")
    void updateStrategy_HttpClientError_MessageParseFail() {
        // given
        Map<String, Object> strategyData = new HashMap<>();
        strategyData.put("name", "Test Strategy");

        // JSON이 아닌 응답
        when(restTemplate.exchange(
                eq("/api/v1/admin/strategies/strategy-001"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid JSON response without message key".getBytes(), null));

        // when & then
        assertThatThrownBy(() -> tradingApiService.updateStrategy("strategy-001", strategyData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Trading System API 요청 오류");
    }

    @Test
    @DisplayName("deleteBacktest - API 실패 시")
    void deleteBacktest_ApiFailure() {
        // given
        when(restTemplate.exchange(
                eq("/api/v1/admin/backtests/backtest-001"),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // when
        Map<String, Object> result = tradingApiService.deleteBacktest("backtest-001");

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("백테스트 삭제에 실패했습니다.");
    }

    @Test
    @DisplayName("getFills - accountId가 빈 문자열일 때")
    void getFills_EmptyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - orderId가 빈 문자열일 때")
    void getFills_EmptyOrderId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("account-001", "", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFills - symbol이 빈 문자열일 때")
    void getFills_EmptySymbol() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFills("account-001", "order-001", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - accountId가 빈 문자열일 때")
    void getFillsWithFilters_EmptyAccountId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("", "2024-01-01", "2024-12-31", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - startDate가 빈 문자열일 때")
    void getFillsWithFilters_EmptyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "", "2024-12-31", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - endDate가 빈 문자열일 때")
    void getFillsWithFilters_EmptyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "2024-01-01", "", "order-001", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - orderId가 빈 문자열일 때")
    void getFillsWithFilters_EmptyOrderId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "2024-01-01", "2024-12-31", "", "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - symbol이 빈 문자열일 때")
    void getFillsWithFilters_EmptySymbol() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters("account-001", "2024-01-01", "2024-12-31", "order-001", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getFillsWithFilters - symbol만 있을 때 (hasParam이 false인 상태에서)")
    void getFillsWithFilters_OnlySymbol() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("fills", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getFillsWithFilters(null, null, null, null, "005930");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - strategyId가 빈 문자열일 때")
    void getBacktestResults_EmptyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults("", "2024-01-01", "2024-12-31");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - startDate가 빈 문자열일 때")
    void getBacktestResults_EmptyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults("strategy-001", "", "2024-12-31");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getBacktestResults - endDate가 빈 문자열일 때")
    void getBacktestResults_EmptyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("backtests", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getBacktestResults("strategy-001", "2024-01-01", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - strategyId가 빈 문자열일 때")
    void getExecutionHistory_EmptyStrategyId() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("", "2024-01-01", "2024-12-31", "SUCCESS");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - startDate가 빈 문자열일 때")
    void getExecutionHistory_EmptyStartDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("strategy-001", "", "2024-12-31", "SUCCESS");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - endDate가 빈 문자열일 때")
    void getExecutionHistory_EmptyEndDate() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("strategy-001", "2024-01-01", "", "SUCCESS");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExecutionHistory - status가 빈 문자열일 때")
    void getExecutionHistory_EmptyStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("executions", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getExecutionHistory("strategy-001", "2024-01-01", "2024-12-31", "");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - market가 빈 문자열일 때")
    void getInstruments_EmptyMarket() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("", "ACTIVE", true, "Samsung");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - status가 빈 문자열일 때")
    void getInstruments_EmptyStatus() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("KOSPI", "", true, "Samsung");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getInstruments - search가 빈 문자열일 때")
    void getInstruments_EmptySearch() {
        // given
        Map<String, Object> response = new HashMap<>();
        response.put("items", new java.util.ArrayList<>());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // when
        Map<String, Object> result = tradingApiService.getInstruments("KOSPI", "ACTIVE", true, "");

        // then
        assertThat(result).isNotNull();
    }
}
