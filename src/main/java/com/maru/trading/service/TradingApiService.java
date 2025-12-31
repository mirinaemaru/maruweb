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
}
