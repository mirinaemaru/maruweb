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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
}
