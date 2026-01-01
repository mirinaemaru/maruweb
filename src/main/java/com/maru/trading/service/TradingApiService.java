package com.maru.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Trading System API Service
 * Trading System (port 8099)의 REST API를 호출하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingApiService {

    private final RestTemplate tradingApiRestTemplate;

    /**
     * Health Check - 시스템 상태 조회
     */
    public Map<String, Object> getHealthStatus() {
        String url = "/health";
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get health status from Trading System", e);
            // Return error status
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "DOWN");
            errorStatus.put("error", "Trading System API is unavailable");
            return errorStatus;
        }
    }

    /**
     * 계좌 목록 조회
     */
    public Map<String, Object> getAccounts() {
        String url = "/api/v1/admin/accounts";
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get accounts from Trading System", e);
            throw new RuntimeException("계좌 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 계좌 상세 조회
     */
    public Map<String, Object> getAccount(String accountId) {
        String url = "/api/v1/admin/accounts/" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get account from Trading System", e);
            throw new RuntimeException("계좌 정보를 가져올 수 없습니다.", e);
        }
    }

    /**
     * 계좌 등록
     */
    public Map<String, Object> createAccount(Map<String, Object> accountData) {
        String url = "/api/v1/admin/accounts";
        try {
            log.debug("Calling Trading API: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(accountData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to create account in Trading System", e);
            throw new RuntimeException("계좌 등록에 실패했습니다.", e);
        }
    }

    /**
     * 계좌 수정
     */
    public Map<String, Object> updateAccount(String accountId, Map<String, Object> accountData) {
        String url = "/api/v1/admin/accounts/" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(accountData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update account in Trading System", e);
            throw new RuntimeException("계좌 수정에 실패했습니다.", e);
        }
    }

    /**
     * 계좌 삭제
     */
    public void deleteAccount(String accountId) {
        String url = "/api/v1/admin/accounts/" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        } catch (RestClientException e) {
            log.error("Failed to delete account from Trading System", e);
            throw new RuntimeException("계좌 삭제에 실패했습니다.", e);
        }
    }

    /**
     * Kill Switch 상태 조회
     */
    public Map<String, Object> getKillSwitchStatus() {
        String url = "/api/v1/admin/kill-switch";
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get kill switch status from Trading System", e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "UNKNOWN");
            errorStatus.put("error", "Unable to fetch kill switch status");
            return errorStatus;
        }
    }

    /**
     * Kill Switch 토글 (ON/OFF)
     */
    public Map<String, Object> toggleKillSwitch(String status, String reason, String accountId) {
        String url = "/api/v1/admin/kill-switch";
        try {
            log.info("Toggling Kill Switch: status={}, reason={}, accountId={}", status, reason, accountId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("status", status);
            if (reason != null && !reason.isEmpty()) {
                requestData.put("reason", reason);
            }
            if (accountId != null && !accountId.isEmpty()) {
                requestData.put("accountId", accountId);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to toggle kill switch in Trading System", e);
            throw new RuntimeException("Kill Switch 변경에 실패했습니다.", e);
        }
    }

    /**
     * 전략 목록 조회
     */
    public Map<String, Object> getStrategies() {
        String url = "/api/v1/admin/strategies";
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get strategies from Trading System", e);
            throw new RuntimeException("전략 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 주문 목록 조회
     */
    public Map<String, Object> getOrders(String accountId) {
        String url = "/api/v1/query/orders";
        if (accountId != null && !accountId.isEmpty()) {
            url += "?accountId=" + accountId;
        }
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get orders from Trading System", e);
            throw new RuntimeException("주문 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 주문 목록 조회 (고급 필터링)
     */
    public Map<String, Object> getOrdersWithFilters(String accountId, String startDate, String endDate,
                                                     String status, String symbol, String side) {
        StringBuilder url = new StringBuilder("/api/v1/query/orders?");
        boolean hasParam = false;

        if (accountId != null && !accountId.isEmpty()) {
            url.append("accountId=").append(accountId);
            hasParam = true;
        }
        if (startDate != null && !startDate.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("startDate=").append(startDate);
            hasParam = true;
        }
        if (endDate != null && !endDate.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("endDate=").append(endDate);
            hasParam = true;
        }
        if (status != null && !status.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("status=").append(status);
            hasParam = true;
        }
        if (symbol != null && !symbol.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("symbol=").append(symbol);
            hasParam = true;
        }
        if (side != null && !side.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("side=").append(side);
        }

        try {
            log.debug("Calling Trading API with filters: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get filtered orders from Trading System", e);
            throw new RuntimeException("주문 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 포지션 목록 조회
     */
    public Map<String, Object> getPositions(String accountId) {
        String url = "/api/v1/query/positions?accountId=" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get positions from Trading System", e);
            throw new RuntimeException("포지션 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 계좌 잔액 조회
     */
    public Map<String, Object> getAccountBalance(String accountId) {
        String url = "/api/v1/query/balance?accountId=" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get account balance from Trading System", e);
            throw new RuntimeException("계좌 잔액을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 전략 상세 조회
     */
    public Map<String, Object> getStrategy(String strategyId) {
        String url = "/api/v1/admin/strategies/" + strategyId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get strategy from Trading System", e);
            throw new RuntimeException("전략 정보를 가져올 수 없습니다.", e);
        }
    }

    /**
     * 전략 등록
     */
    public Map<String, Object> createStrategy(Map<String, Object> strategyData) {
        String url = "/api/v1/admin/strategies";
        try {
            log.debug("Calling Trading API: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(strategyData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to create strategy in Trading System", e);
            throw new RuntimeException("전략 등록에 실패했습니다.", e);
        }
    }

    /**
     * 전략 수정
     */
    public Map<String, Object> updateStrategy(String strategyId, Map<String, Object> strategyData) {
        String url = "/api/v1/admin/strategies/" + strategyId;
        try {
            log.debug("Calling Trading API: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(strategyData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update strategy in Trading System", e);
            throw new RuntimeException("전략 수정에 실패했습니다.", e);
        }
    }

    /**
     * 전략 삭제
     */
    public void deleteStrategy(String strategyId) {
        String url = "/api/v1/admin/strategies/" + strategyId;
        try {
            log.debug("Calling Trading API: {}", url);
            tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        } catch (RestClientException e) {
            log.error("Failed to delete strategy from Trading System", e);
            throw new RuntimeException("전략 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 전략 상태 변경
     */
    public Map<String, Object> updateStrategyStatus(String strategyId, String status) {
        String url = "/api/v1/admin/strategies/" + strategyId + "/status";
        try {
            log.debug("Calling Trading API: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(statusData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update strategy status in Trading System", e);
            throw new RuntimeException("전략 상태 변경에 실패했습니다.", e);
        }
    }

    /**
     * 체결 내역 조회
     */
    public Map<String, Object> getFills(String accountId, String orderId, String symbol) {
        StringBuilder url = new StringBuilder("/api/v1/query/fills?");

        boolean hasParam = false;
        if (accountId != null && !accountId.isEmpty()) {
            url.append("accountId=").append(accountId);
            hasParam = true;
        }
        if (orderId != null && !orderId.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("orderId=").append(orderId);
            hasParam = true;
        }
        if (symbol != null && !symbol.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("symbol=").append(symbol);
        }

        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get fills from Trading System", e);
            throw new RuntimeException("체결 내역을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 체결 내역 조회 (고급 필터링)
     */
    public Map<String, Object> getFillsWithFilters(String accountId, String startDate, String endDate,
                                                    String orderId, String symbol) {
        StringBuilder url = new StringBuilder("/api/v1/query/fills?");

        boolean hasParam = false;
        if (accountId != null && !accountId.isEmpty()) {
            url.append("accountId=").append(accountId);
            hasParam = true;
        }
        if (startDate != null && !startDate.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("startDate=").append(startDate);
            hasParam = true;
        }
        if (endDate != null && !endDate.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("endDate=").append(endDate);
            hasParam = true;
        }
        if (orderId != null && !orderId.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("orderId=").append(orderId);
            hasParam = true;
        }
        if (symbol != null && !symbol.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("symbol=").append(symbol);
        }

        try {
            log.debug("Calling Trading API with filters: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get filtered fills from Trading System", e);
            throw new RuntimeException("체결 내역을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 계좌 잔고 조회
     */
    public Map<String, Object> getBalance(String accountId) {
        String url = "/api/v1/query/balance?accountId=" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get balance from Trading System", e);
            throw new RuntimeException("계좌 잔고를 가져올 수 없습니다.", e);
        }
    }

    /**
     * 주문 취소
     */
    public Map<String, Object> cancelOrder(String orderId) {
        String url = "/api/v1/admin/orders/cancel";
        try {
            log.info("Cancelling order: {}", orderId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("orderId", orderId);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to cancel order in Trading System", e);
            throw new RuntimeException("주문 취소에 실패했습니다.", e);
        }
    }

    /**
     * 주문 수정
     */
    public Map<String, Object> modifyOrder(String orderId, Double newPrice, Integer newQuantity) {
        String url = "/api/v1/admin/orders/modify";
        try {
            log.info("Modifying order: orderId={}, newPrice={}, newQuantity={}", orderId, newPrice, newQuantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("orderId", orderId);
            if (newPrice != null) {
                requestData.put("newPrice", newPrice);
            }
            if (newQuantity != null) {
                requestData.put("newQuantity", newQuantity);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to modify order in Trading System", e);
            throw new RuntimeException("주문 수정에 실패했습니다.", e);
        }
    }

    /**
     * 리스크 룰 조회 (계좌별)
     */
    public Map<String, Object> getRiskRulesForAccount(String accountId) {
        String url = "/api/v1/admin/risk-rules/account/" + accountId;
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get risk rules from Trading System", e);
            throw new RuntimeException("리스크 룰을 가져올 수 없습니다.", e);
        }
    }

    /**
     * 전역 리스크 룰 업데이트
     */
    public Map<String, Object> updateGlobalRiskRule(Map<String, Object> ruleData) {
        String url = "/api/v1/admin/risk-rules/global";
        try {
            log.info("Updating global risk rule");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(ruleData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update global risk rule in Trading System", e);
            throw new RuntimeException("전역 리스크 룰 업데이트에 실패했습니다.", e);
        }
    }

    /**
     * 계좌별 리스크 룰 업데이트
     */
    public Map<String, Object> updateAccountRiskRule(String accountId, Map<String, Object> ruleData) {
        String url = "/api/v1/admin/risk-rules/account/" + accountId;
        try {
            log.info("Updating risk rule for account: {}", accountId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(ruleData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update account risk rule in Trading System", e);
            throw new RuntimeException("계좌 리스크 룰 업데이트에 실패했습니다.", e);
        }
    }

    /**
     * 종목별 리스크 룰 업데이트
     */
    public Map<String, Object> updateSymbolRiskRule(String accountId, String symbol, Map<String, Object> ruleData) {
        String url = "/api/v1/admin/risk-rules/account/" + accountId + "/symbol/" + symbol;
        try {
            log.info("Updating risk rule for account: {}, symbol: {}", accountId, symbol);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(ruleData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update symbol risk rule in Trading System", e);
            throw new RuntimeException("종목 리스크 룰 업데이트에 실패했습니다.", e);
        }
    }

    /**
     * 리스크 룰 삭제
     */
    public void deleteRiskRule(String ruleId) {
        String url = "/api/v1/admin/risk-rules/" + ruleId;
        try {
            log.info("Deleting risk rule: {}", ruleId);
            tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        } catch (RestClientException e) {
            log.error("Failed to delete risk rule in Trading System", e);
            throw new RuntimeException("리스크 룰 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 데모 신호 주입
     */
    public Map<String, Object> injectDemoSignal(Map<String, Object> signalData) {
        String url = "/api/v1/demo/signal";
        try {
            log.info("Injecting demo signal: {}", signalData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(signalData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to inject demo signal in Trading System", e);
            throw new RuntimeException("데모 신호 주입에 실패했습니다.", e);
        }
    }

    /**
     * 데모 시나리오 목록 조회
     */
    public Map<String, Object> getDemoScenarios() {
        String url = "/api/v1/demo/scenarios";
        try {
            log.debug("Calling Trading API: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get demo scenarios from Trading System", e);
            throw new RuntimeException("데모 시나리오 목록을 가져올 수 없습니다.", e);
        }
    }

    /**
     * Golden Cross 데모 시나리오 실행
     */
    public Map<String, Object> runGoldenCrossDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/golden-cross";
        try {
            log.info("Running Golden Cross demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Golden Cross demo in Trading System", e);
            throw new RuntimeException("Golden Cross 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * Death Cross 데모 시나리오 실행
     */
    public Map<String, Object> runDeathCrossDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/death-cross";
        try {
            log.info("Running Death Cross demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Death Cross demo in Trading System", e);
            throw new RuntimeException("Death Cross 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * RSI Oversold 데모 시나리오 실행
     */
    public Map<String, Object> runRsiOversoldDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/rsi-oversold";
        try {
            log.info("Running RSI Oversold demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run RSI Oversold demo in Trading System", e);
            throw new RuntimeException("RSI Oversold 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * RSI Overbought 데모 시나리오 실행
     */
    public Map<String, Object> runRsiOverboughtDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/rsi-overbought";
        try {
            log.info("Running RSI Overbought demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run RSI Overbought demo in Trading System", e);
            throw new RuntimeException("RSI Overbought 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * Volatile 데모 시나리오 실행
     */
    public Map<String, Object> runVolatileDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/volatile";
        try {
            log.info("Running Volatile demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Volatile demo in Trading System", e);
            throw new RuntimeException("Volatile 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * Stable 데모 시나리오 실행
     */
    public Map<String, Object> runStableDemo(Map<String, Object> params) {
        String url = "/api/v1/demo/stable";
        try {
            log.info("Running Stable demo scenario: {}", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Stable demo in Trading System", e);
            throw new RuntimeException("Stable 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * 커스텀 데모 시나리오 실행
     */
    public Map<String, Object> runCustomDemo(Map<String, Object> scenarioData) {
        String url = "/api/v1/demo/run";
        try {
            log.info("Running custom demo scenario: {}", scenarioData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(scenarioData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run custom demo in Trading System", e);
            throw new RuntimeException("커스텀 데모 실행에 실패했습니다.", e);
        }
    }

    /**
     * 전략 수동 실행
     */
    public Map<String, Object> executeStrategy(String strategyId, String symbol, String accountId) {
        String url = "/api/v1/admin/strategies/" + strategyId + "/execute?symbol=" + symbol + "&accountId=" + accountId;
        try {
            log.info("Executing strategy manually: strategyId={}, symbol={}, accountId={}", strategyId, symbol, accountId);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to execute strategy in Trading System", e);
            throw new RuntimeException("전략 수동 실행에 실패했습니다.", e);
        }
    }

    /**
     * 계좌 상태 업데이트
     */
    public Map<String, Object> updateAccountStatus(String accountId, String status) {
        String url = "/api/v1/admin/accounts/" + accountId + "/status";
        try {
            log.info("Updating account status: accountId={}, status={}", accountId, status);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(statusData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update account status in Trading System", e);
            throw new RuntimeException("계좌 상태 업데이트에 실패했습니다.", e);
        }
    }

    /**
     * 대시보드 통계 조회
     */
    public Map<String, Object> getDashboardStats() {
        String url = "/api/v1/query/dashboard/stats";
        try {
            log.debug("Calling Trading API for dashboard stats: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get dashboard stats from Trading System", e);
            // Return empty stats
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("todayOrders", 0);
            emptyStats.put("todayFills", 0);
            emptyStats.put("todayProfitLoss", 0);
            emptyStats.put("totalProfitLoss", 0);
            emptyStats.put("winRate", 0.0);
            emptyStats.put("recentActivities", new java.util.ArrayList<>());
            emptyStats.put("dailyStats", new java.util.ArrayList<>());
            return emptyStats;
        }
    }

    /**
     * 백테스팅 결과 목록 조회
     */
    public Map<String, Object> getBacktestResults(String strategyId, String startDate, String endDate) {
        StringBuilder url = new StringBuilder("/api/v1/query/backtests?");

        if (strategyId != null && !strategyId.isEmpty()) {
            url.append("strategyId=").append(strategyId).append("&");
        }
        if (startDate != null && !startDate.isEmpty()) {
            url.append("startDate=").append(startDate).append("&");
        }
        if (endDate != null && !endDate.isEmpty()) {
            url.append("endDate=").append(endDate).append("&");
        }

        String finalUrl = url.toString().replaceAll("[&?]$", "");

        try {
            log.debug("Calling Trading API for backtest results: {}", finalUrl);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get backtest results from Trading System", e);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("backtests", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * 백테스팅 결과 상세 조회
     */
    public Map<String, Object> getBacktestDetail(Long id) {
        String url = "/api/v1/query/backtests/" + id;
        try {
            log.debug("Calling Trading API for backtest detail: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get backtest detail from Trading System", e);
            Map<String, Object> emptyDetail = new HashMap<>();
            emptyDetail.put("id", id);
            emptyDetail.put("trades", new java.util.ArrayList<>());
            emptyDetail.put("metrics", new HashMap<>());
            emptyDetail.put("equityCurve", new java.util.ArrayList<>());
            return emptyDetail;
        }
    }

    /**
     * 전략 실행 히스토리 조회
     */
    public Map<String, Object> getExecutionHistory(String strategyId, String startDate, String endDate, String status) {
        StringBuilder url = new StringBuilder("/api/v1/query/executions?");

        if (strategyId != null && !strategyId.isEmpty()) {
            url.append("strategyId=").append(strategyId).append("&");
        }
        if (startDate != null && !startDate.isEmpty()) {
            url.append("startDate=").append(startDate).append("&");
        }
        if (endDate != null && !endDate.isEmpty()) {
            url.append("endDate=").append(endDate).append("&");
        }
        if (status != null && !status.isEmpty()) {
            url.append("status=").append(status).append("&");
        }

        // Remove trailing '&' or '?' if present
        String finalUrl = url.toString().replaceAll("[&?]$", "");

        try {
            log.debug("Calling Trading API for execution history: {}", finalUrl);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get execution history from Trading System", e);
            // Return empty result instead of throwing exception
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("executions", new java.util.ArrayList<>());
            emptyResult.put("totalExecutions", 0);
            emptyResult.put("successfulExecutions", 0);
            emptyResult.put("failedExecutions", 0);
            emptyResult.put("totalProfitLoss", 0);
            return emptyResult;
        }
    }

    /**
     * Walk-Forward Analysis 실행
     */
    public Map<String, Object> runWalkForwardAnalysis(Map<String, Object> request) {
        String url = "/api/v1/demo/advanced/walk-forward";
        try {
            log.debug("Calling Trading API for walk-forward analysis: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run walk-forward analysis", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Walk-Forward Analysis 실행에 실패했습니다.");
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Portfolio Backtest 실행
     */
    public Map<String, Object> runPortfolioBacktest(Map<String, Object> request) {
        String url = "/api/v1/demo/advanced/portfolio";
        try {
            log.debug("Calling Trading API for portfolio backtest: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run portfolio backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Portfolio Backtest 실행에 실패했습니다.");
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 백테스트 거래 상세 조회
     */
    public Map<String, Object> getBacktestTrades(Long backtestId) {
        String url = "/api/v1/admin/backtests/" + backtestId + "/trades";
        try {
            log.debug("Calling Trading API for backtest trades: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get backtest trades", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "백테스트 거래 내역을 가져올 수 없습니다.");
            errorResult.put("trades", new java.util.ArrayList<>());
            return errorResult;
        }
    }

    /**
     * 성과 분석 조회 (일별/월별)
     */
    public Map<String, Object> getPerformanceAnalysis(String period, String startDate, String endDate, String strategyId) {
        String url = "/api/v1/query/performance/analysis";
        try {
            Map<String, String> params = new HashMap<>();
            params.put("period", period);
            params.put("startDate", startDate);
            params.put("endDate", endDate);
            if (strategyId != null && !strategyId.isEmpty()) {
                params.put("strategyId", strategyId);
            }

            StringBuilder urlBuilder = new StringBuilder(url).append("?");
            params.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                urlBuilder.toString(), HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get performance analysis", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "성과 분석 조회에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 전략별 통계 조회
     */
    public Map<String, Object> getStrategyStatistics(String startDate, String endDate) {
        String url = String.format("/api/v1/query/performance/strategies?startDate=%s&endDate=%s",
            startDate, endDate);
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get strategy statistics", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "전략 통계 조회에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 그리드 서치 실행
     */
    public Map<String, Object> runGridSearch(Map<String, Object> request) {
        String url = "/api/v1/optimization/grid-search";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run grid search", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "그리드 서치 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 최적화 결과 조회
     */
    public Map<String, Object> getOptimizationResults(String strategyId) {
        String url = "/api/v1/optimization/results";
        if (strategyId != null && !strategyId.isEmpty()) {
            url += "?strategyId=" + strategyId;
        }
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get optimization results", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "최적화 결과 조회에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 유전 알고리즘 실행
     */
    public Map<String, Object> runGeneticAlgorithm(Map<String, Object> request) {
        String url = "/api/v1/optimization/genetic-algorithm";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run genetic algorithm", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "유전 알고리즘 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * VaR 계산
     */
    public Map<String, Object> calculateVaR(String strategyId, double confidenceLevel, int timeHorizon, String startDate, String endDate) {
        String url = String.format("/api/v1/risk/var?strategyId=%s&confidenceLevel=%s&timeHorizon=%s&startDate=%s&endDate=%s",
            strategyId, confidenceLevel, timeHorizon, startDate, endDate);
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to calculate VaR", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "VaR 계산에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 상관관계 분석
     */
    public Map<String, Object> getCorrelationAnalysis(String startDate, String endDate) {
        String url = String.format("/api/v1/risk/correlation?startDate=%s&endDate=%s", startDate, endDate);
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get correlation analysis", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "상관관계 분석에 실패했습니다.");
            return errorResult;
        }
    }
}
