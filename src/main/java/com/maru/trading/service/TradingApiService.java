package com.maru.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
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
}
