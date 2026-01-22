package com.maru.trading.service;

import com.maru.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
    private final TradingApiHelper apiHelper;

    /**
     * Health Check - 시스템 상태 조회
     */
    @Cacheable(value = CacheConfig.CACHE_HEALTH, cacheManager = "shortTtlCacheManager")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> defaultStatus = new HashMap<>();
        defaultStatus.put("status", "DOWN");
        defaultStatus.put("error", "Trading System API is unavailable");
        return apiHelper.getWithDefault("/health", defaultStatus);
    }

    /**
     * 계좌 목록 조회
     */
    @Cacheable(value = CacheConfig.CACHE_ACCOUNTS)
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
    @Cacheable(value = CacheConfig.CACHE_ACCOUNTS, key = "#accountId")
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
    @CacheEvict(value = CacheConfig.CACHE_ACCOUNTS, allEntries = true)
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
    @CacheEvict(value = CacheConfig.CACHE_ACCOUNTS, allEntries = true)
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
    @CacheEvict(value = CacheConfig.CACHE_ACCOUNTS, allEntries = true)
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
    @Cacheable(value = "killSwitch", cacheManager = "shortTtlCacheManager")
    public Map<String, Object> getKillSwitchStatus() {
        Map<String, Object> defaultStatus = new HashMap<>();
        defaultStatus.put("status", "UNKNOWN");
        defaultStatus.put("error", "Unable to fetch kill switch status");
        return apiHelper.getWithDefault("/api/v1/admin/kill-switch", defaultStatus);
    }

    /**
     * Kill Switch 토글 (ON/OFF)
     */
    @CacheEvict(value = "killSwitch", allEntries = true)
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
    @Cacheable(value = CacheConfig.CACHE_STRATEGIES)
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
     * 주문 상세 조회 (ID 기반)
     */
    public Map<String, Object> getOrder(String orderId) {
        String url = "/api/v1/query/orders/" + orderId;
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
            log.error("Failed to get order from Trading System: {}", orderId, e);
            throw new RuntimeException("주문 정보를 가져올 수 없습니다.", e);
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
        return getOrdersWithFilters(accountId, startDate, endDate, status, symbol, side, null);
    }

    /**
     * 주문 목록 조회 (고급 필터링 + strategyId)
     */
    public Map<String, Object> getOrdersWithFilters(String accountId, String startDate, String endDate,
                                                     String status, String symbol, String side, String strategyId) {
        StringBuilder url = new StringBuilder("/api/v1/query/orders?");
        boolean hasParam = false;

        if (accountId != null && !accountId.isEmpty()) {
            url.append("accountId=").append(accountId);
            hasParam = true;
        }
        if (strategyId != null && !strategyId.isEmpty()) {
            if (hasParam) url.append("&");
            url.append("strategyId=").append(strategyId);
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
     * 전략별 주문 목록 조회
     */
    public Map<String, Object> getOrdersByStrategyId(String strategyId) {
        return getOrdersWithFilters(null, null, null, null, null, null, strategyId);
    }

    /**
     * 포지션 상세 조회 (ID 기반)
     */
    public Map<String, Object> getPosition(String positionId) {
        String url = "/api/v1/query/positions/" + positionId;
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
            log.error("Failed to get position from Trading System: {}", positionId, e);
            throw new RuntimeException("포지션 정보를 가져올 수 없습니다.", e);
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
    @Cacheable(value = CacheConfig.CACHE_STRATEGIES, key = "#strategyId")
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
    @CacheEvict(value = CacheConfig.CACHE_STRATEGIES, allEntries = true)
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
    @CacheEvict(value = CacheConfig.CACHE_STRATEGIES, allEntries = true)
    public Map<String, Object> updateStrategy(String strategyId, Map<String, Object> strategyData) {
        String url = "/api/v1/admin/strategies/" + strategyId;
        try {
            log.debug("Calling Trading API: {}", url);
            log.debug("Strategy data: {}", strategyData);
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
        } catch (HttpServerErrorException e) {
            log.error("Server error from Trading System API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            String errorDetail = extractErrorMessage(e.getResponseBodyAsString());
            throw new RuntimeException("Trading System API 서버 오류: " + errorDetail, e);
        } catch (HttpClientErrorException e) {
            log.error("Client error from Trading System API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            String errorDetail = extractErrorMessage(e.getResponseBodyAsString());
            throw new RuntimeException("Trading System API 요청 오류: " + errorDetail, e);
        } catch (RestClientException e) {
            log.error("Failed to update strategy in Trading System", e);
            throw new RuntimeException("Trading System에 연결할 수 없습니다.", e);
        }
    }

    /**
     * Extract error message from Trading System API error response
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "알 수 없는 오류";
        }
        try {
            // Try to parse JSON error response
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\":\"") + 11;
                int end = responseBody.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return responseBody.substring(start, end);
                }
            }
            if (responseBody.contains("\"detail\"")) {
                int start = responseBody.indexOf("\"detail\":\"") + 10;
                int end = responseBody.indexOf("\"", start);
                if (start > 9 && end > start) {
                    return responseBody.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse error response: {}", responseBody);
        }
        return responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody;
    }

    /**
     * 전략 삭제
     */
    @CacheEvict(value = CacheConfig.CACHE_STRATEGIES, allEntries = true)
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
    @CacheEvict(value = CacheConfig.CACHE_STRATEGIES, allEntries = true)
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
     * 체결 상세 조회 (ID 기반)
     */
    public Map<String, Object> getFill(String fillId) {
        String url = "/api/v1/query/fills/" + fillId;
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
            log.error("Failed to get fill from Trading System: {}", fillId, e);
            throw new RuntimeException("체결 정보를 가져올 수 없습니다.", e);
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
        StringBuilder url = new StringBuilder("/api/v1/admin/backtests?");

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

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("content")) {
                // API 응답의 "content" 키를 "backtests"로 매핑
                Map<String, Object> result = new HashMap<>();
                result.put("backtests", body.get("content"));
                result.put("totalElements", body.get("totalElements"));
                result.put("page", body.get("page"));
                result.put("totalPages", body.get("totalPages"));
                return result;
            }
            return body != null ? body : new HashMap<>();
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
     * 월별 일일 성과 데이터 조회 (캘린더용)
     */
    public Map<String, Object> getMonthlyDailyPerformance(int year, int month, String strategyId) {
        StringBuilder url = new StringBuilder("/api/v1/query/performance/monthly-daily?");
        url.append("year=").append(year);
        url.append("&month=").append(month);
        if (strategyId != null && !strategyId.isEmpty()) {
            url.append("&strategyId=").append(strategyId);
        }

        try {
            log.debug("Calling Trading API for monthly daily performance: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url.toString(), HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get monthly daily performance", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "월별 일일 성과 조회에 실패했습니다.");
            errorResult.put("dailyData", new java.util.ArrayList<>());
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

    // ==================== 종목 관리 (Instrument Management) ====================

    /**
     * 종목 목록 조회
     */
    @Cacheable(value = CacheConfig.CACHE_INSTRUMENTS, cacheManager = "longTtlCacheManager",
               key = "T(java.util.Objects).hash(#market, #status, #tradable, #search)")
    public Map<String, Object> getInstruments(String market, String status, Boolean tradable, String search) {
        Map<String, String> params = new HashMap<>();
        if (market != null && !market.isEmpty()) params.put("market", market);
        if (status != null && !status.isEmpty()) params.put("status", status);
        if (tradable != null) params.put("tradable", tradable.toString());
        if (search != null && !search.isEmpty()) params.put("search", search);

        String url = apiHelper.buildUrl("/api/v1/admin/instruments", params);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("items", new java.util.ArrayList<>());
        defaultResult.put("total", 0);
        defaultResult.put("error", "종목 목록을 가져올 수 없습니다.");
        return apiHelper.getWithDefault(url, defaultResult);
    }

    /**
     * 종목 상세 조회
     */
    @Cacheable(value = CacheConfig.CACHE_INSTRUMENTS, cacheManager = "longTtlCacheManager", key = "#symbol")
    public Map<String, Object> getInstrument(String symbol) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("error", "종목 정보를 가져올 수 없습니다.");
        return apiHelper.getWithDefault("/api/v1/admin/instruments/" + symbol, defaultResult);
    }

    /**
     * 종목 상태 업데이트
     */
    @CacheEvict(value = CacheConfig.CACHE_INSTRUMENTS, allEntries = true)
    public Map<String, Object> updateInstrumentStatus(String symbol, String status, Boolean tradable, Boolean halted) {
        String url = "/api/v1/admin/instruments/" + symbol + "/status";
        try {
            log.info("Updating instrument status: symbol={}, status={}, tradable={}, halted={}", symbol, status, tradable, halted);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> statusData = new HashMap<>();
            if (status != null) statusData.put("status", status);
            if (tradable != null) statusData.put("tradable", tradable);
            if (halted != null) statusData.put("halted", halted);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(statusData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.PUT, request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to update instrument status", e);
            throw new RuntimeException("종목 상태 업데이트에 실패했습니다.", e);
        }
    }

    // ==================== 백테스트 관리 (Admin Backtest) ====================

    /**
     * 백테스트 실행
     */
    public Map<String, Object> runBacktest(Map<String, Object> request) {
        String url = "/api/v1/admin/backtests";
        try {
            log.info("Running backtest: {}", request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "백테스트 실행에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 백테스트 목록 조회 (Admin)
     */
    public Map<String, Object> listBacktests() {
        String url = "/api/v1/admin/backtests";
        try {
            log.debug("Calling Trading API for backtest list: {}", url);
            ResponseEntity<Object> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Object>() {}
            );
            // Handle both List and Map responses
            Object body = response.getBody();
            Map<String, Object> result = new HashMap<>();
            if (body instanceof java.util.List) {
                result.put("backtests", body);
            } else if (body instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = (Map<String, Object>) body;
                // API 응답의 "content" 키를 "backtests"로 매핑
                if (bodyMap.containsKey("content")) {
                    result.put("backtests", bodyMap.get("content"));
                    result.put("totalElements", bodyMap.get("totalElements"));
                } else {
                    result = bodyMap;
                }
            }
            return result;
        } catch (RestClientException e) {
            log.error("Failed to list backtests", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("backtests", new java.util.ArrayList<>());
            errorResult.put("error", "백테스트 목록을 가져올 수 없습니다.");
            return errorResult;
        }
    }

    /**
     * 백테스트 삭제
     */
    public Map<String, Object> deleteBacktest(String backtestId) {
        String url = "/api/v1/admin/backtests/" + backtestId;
        try {
            log.info("Deleting backtest: {}", backtestId);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to delete backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "백테스트 삭제에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 백테스트 상세 조회 (Admin)
     */
    public Map<String, Object> getBacktest(String backtestId) {
        String url = "/api/v1/admin/backtests/" + backtestId;
        try {
            log.debug("Calling Trading API for backtest: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "백테스트 정보를 가져올 수 없습니다.");
            return errorResult;
        }
    }

    // ==================== 최적화 데모 (Optimization Demo) ====================

    /**
     * MA 크로스오버 최적화 데모
     */
    public Map<String, Object> runMACrossoverOptimization() {
        String url = "/api/v1/demo/optimization/ma-crossover";
        try {
            log.info("Running MA Crossover optimization demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run MA Crossover optimization", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "MA 크로스오버 최적화 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * RSI 최적화 데모
     */
    public Map<String, Object> runRSIOptimization() {
        String url = "/api/v1/demo/optimization/rsi";
        try {
            log.info("Running RSI optimization demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run RSI optimization", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "RSI 최적화 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 랜덤 서치 최적화 데모
     */
    public Map<String, Object> runRandomSearchOptimization() {
        String url = "/api/v1/demo/advanced/random-search";
        try {
            log.info("Running Random Search optimization demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Random Search optimization", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "랜덤 서치 최적화 실행에 실패했습니다.");
            return errorResult;
        }
    }

    // ==================== 데모 백테스트 (Demo Backtest) ====================

    /**
     * 데모 데이터 생성
     */
    public Map<String, Object> generateDemoData() {
        String url = "/api/v1/demo/backtest/generate-data";
        try {
            log.info("Generating demo data");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to generate demo data", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "데모 데이터 생성에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * MA 크로스오버 백테스트 데모
     */
    public Map<String, Object> runMACrossoverBacktest() {
        String url = "/api/v1/demo/backtest/ma-crossover";
        try {
            log.info("Running MA Crossover backtest demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run MA Crossover backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "MA 크로스오버 백테스트 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * RSI 백테스트 데모
     */
    public Map<String, Object> runRSIBacktest() {
        String url = "/api/v1/demo/backtest/rsi";
        try {
            log.info("Running RSI backtest demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run RSI backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "RSI 백테스트 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 백테스트 비교 데모
     */
    public Map<String, Object> runBacktestComparison() {
        String url = "/api/v1/demo/backtest/compare";
        try {
            log.info("Running backtest comparison demo");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run backtest comparison", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "백테스트 비교 실행에 실패했습니다.");
            return errorResult;
        }
    }

    /**
     * 데모 데이터 삭제
     */
    public Map<String, Object> clearDemoData() {
        String url = "/api/v1/demo/backtest/clear";
        try {
            log.info("Clearing demo data");
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to clear demo data", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "데모 데이터 삭제에 실패했습니다.");
            return errorResult;
        }
    }

    // ==================== 계좌 권한 관리 (Account Permission) ====================

    /**
     * 계좌 권한 조회
     */
    public Map<String, Object> getAccountPermission(String accountId) {
        String url = "/api/v1/admin/accounts/" + accountId + "/permissions";
        try {
            log.debug("Calling Trading API for account permission: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Account not found: {}", accountId, e);
                throw new RuntimeException("계좌를 찾을 수 없습니다.", e);
            } else {
                log.error("Client error from Trading System API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("계좌 권한 정보를 가져올 수 없습니다.", e);
            }
        } catch (RestClientException e) {
            log.error("Failed to get account permission from Trading System", e);
            throw new RuntimeException("계좌 권한 정보를 가져올 수 없습니다.", e);
        }
    }

    /**
     * 계좌 권한 업데이트
     */
    public Map<String, Object> updateAccountPermission(String accountId, Map<String, Object> permissionData) {
        String url = "/api/v1/admin/accounts/" + accountId + "/permissions";
        try {
            log.info("Updating account permission: accountId={}, data={}", accountId, permissionData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(permissionData, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Account not found: {}", accountId, e);
                throw new RuntimeException("계좌를 찾을 수 없습니다.", e);
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Invalid permission data: {}", permissionData, e);
                throw new RuntimeException("잘못된 권한 데이터입니다.", e);
            } else {
                log.error("Client error from Trading System API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("계좌 권한 업데이트에 실패했습니다.", e);
            }
        } catch (RestClientException e) {
            log.error("Failed to update account permission in Trading System", e);
            throw new RuntimeException("계좌 권한 업데이트에 실패했습니다.", e);
        }
    }

    // ==================== 비동기 백테스트 (Async Backtest) ====================

    /**
     * 비동기 백테스트 제출
     */
    public Map<String, Object> submitAsyncBacktest(Map<String, Object> request) {
        String url = "/api/v1/admin/backtests/async";
        try {
            log.info("Submitting async backtest: {}", request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to submit async backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "비동기 백테스트 제출에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 비동기 백테스트 작업 상태 조회
     */
    public Map<String, Object> getAsyncBacktestStatus(String jobId) {
        String url = "/api/v1/admin/backtests/jobs/" + jobId + "/status";
        try {
            log.debug("Calling Trading API for async backtest status: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get async backtest status", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "백테스트 작업 상태를 가져올 수 없습니다.");
            errorResult.put("status", "UNKNOWN");
            return errorResult;
        }
    }

    /**
     * 비동기 백테스트 작업 결과 조회
     */
    public Map<String, Object> getAsyncBacktestResult(String jobId) {
        String url = "/api/v1/admin/backtests/jobs/" + jobId + "/result";
        try {
            log.debug("Calling Trading API for async backtest result: {}", url);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to get async backtest result", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "백테스트 결과를 가져올 수 없습니다.");
            return errorResult;
        }
    }

    /**
     * 비동기 백테스트 작업 취소
     */
    public Map<String, Object> cancelAsyncBacktest(String jobId) {
        String url = "/api/v1/admin/backtests/jobs/" + jobId + "/cancel";
        try {
            log.info("Cancelling async backtest job: {}", jobId);
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to cancel async backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "백테스트 취소에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    // ==================== 몬테카를로 시뮬레이션 (Monte Carlo Simulation) ====================

    /**
     * 몬테카를로 시뮬레이션 실행
     */
    public Map<String, Object> runMonteCarloSimulation(Map<String, Object> request) {
        String url = "/api/v1/admin/backtests/monte-carlo";
        try {
            log.info("Running Monte Carlo simulation: {}", request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to run Monte Carlo simulation", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "몬테카를로 시뮬레이션 실행에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }
}
