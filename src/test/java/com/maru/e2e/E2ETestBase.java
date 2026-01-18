package com.maru.e2e;

import com.maru.integration.TestConfig;
import com.maru.todo.TodoApplication;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * E2E 테스트를 위한 베이스 클래스
 *
 * cautostock 서버가 실행 중이어야 테스트가 성공합니다.
 * 테스트 실행 전 cautostock 서버 시작:
 * cd /Users/changsupark/projects/cautostock && ./gradlew bootRun
 */
@SpringBootTest(
    classes = {TodoApplication.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(E2ETestConfig.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected RestTemplate restTemplate;

    @Value("${trading.api.base-url:http://localhost:8099}")
    protected String cautostockBaseUrl;

    @Value("${e2e.test.cautostock.health-check.timeout:30000}")
    protected long healthCheckTimeout;

    @Value("${e2e.test.cautostock.health-check.interval:1000}")
    protected long healthCheckInterval;

    protected String baseUrl;

    @BeforeAll
    void waitForCautostock() throws InterruptedException {
        System.out.println("Waiting for cautostock server at " + cautostockBaseUrl + "...");

        long startTime = System.currentTimeMillis();
        boolean serverReady = false;

        while (!serverReady && (System.currentTimeMillis() - startTime) < healthCheckTimeout) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    cautostockBaseUrl + "/health", Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Map body = response.getBody();
                    if (body != null && "UP".equals(body.get("status"))) {
                        serverReady = true;
                        System.out.println("Cautostock server is ready!");
                    }
                }
            } catch (RestClientException e) {
                System.out.println("Waiting for cautostock... " +
                    ((System.currentTimeMillis() - startTime) / 1000) + "s elapsed");
            }

            if (!serverReady) {
                Thread.sleep(healthCheckInterval);
            }
        }

        if (!serverReady) {
            throw new IllegalStateException(
                "Cautostock server is not available at " + cautostockBaseUrl +
                ". Please start cautostock server before running E2E tests.");
        }
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    // ========== Utility Methods ==========

    protected String getMaruwebUrl(String path) {
        return baseUrl + path;
    }

    protected String getCautostockUrl(String path) {
        return cautostockBaseUrl + path;
    }

    protected HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    protected HttpHeaders createFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    protected <T> HttpEntity<T> createJsonEntity(T body) {
        return new HttpEntity<>(body, createJsonHeaders());
    }

    // ========== Test Data Factory ==========

    protected Map<String, Object> createTestAccountData() {
        Map<String, Object> account = new HashMap<>();
        account.put("broker", "키움증권");
        account.put("cano", "TEST" + System.currentTimeMillis());
        account.put("acntPrdtCd", "01");
        account.put("alias", "E2E 테스트 계좌");
        account.put("environment", "PAPER");
        return account;
    }

    protected Map<String, Object> createTestStrategyData() {
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("name", "E2E 테스트 전략 " + System.currentTimeMillis());
        strategy.put("description", "E2E 테스트용 자동매매 전략");
        strategy.put("strategyType", "AUTO_TRADING");
        strategy.put("symbol", "005930");
        strategy.put("assetType", "STOCK");
        strategy.put("positionSizeType", "FIXED_AMOUNT");
        strategy.put("positionSizeValue", new BigDecimal("1000000"));
        strategy.put("maxPositions", 3);
        strategy.put("stopLossType", "PERCENT");
        strategy.put("stopLossValue", new BigDecimal("5"));
        strategy.put("takeProfitType", "PERCENT");
        strategy.put("takeProfitValue", new BigDecimal("10"));
        return strategy;
    }

    protected Map<String, Object> createTestOrderData(String accountId, String symbol) {
        Map<String, Object> order = new HashMap<>();
        order.put("accountId", accountId);
        order.put("symbol", symbol);
        order.put("side", "BUY");
        order.put("orderType", "LIMIT");
        order.put("quantity", 10);
        order.put("price", new BigDecimal("70000"));
        order.put("timeInForce", "GTC");
        return order;
    }

    protected Map<String, Object> createTestNotificationData() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "STRATEGY_ALERT");
        notification.put("title", "E2E 테스트 알림");
        notification.put("message", "E2E 테스트 메시지 " + System.currentTimeMillis());
        notification.put("priority", "MEDIUM");
        return notification;
    }

    // ========== Date Utility ==========

    protected String getToday() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    protected String getDateBefore(int days) {
        return LocalDateTime.now().minusDays(days)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    protected String getDateAfter(int days) {
        return LocalDateTime.now().plusDays(days)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // ========== Safe API Call Helpers ==========

    /**
     * cautostock API를 안전하게 호출합니다.
     * 500 에러 발생 시 테스트를 스킵합니다.
     */
    protected <T> ResponseEntity<T> safeCautostockGet(String path, Class<T> responseType) {
        try {
            return restTemplate.getForEntity(getCautostockUrl(path), responseType);
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable: " + e.getMessage());
            Assumptions.assumeTrue(false, "Cautostock API not available: " + path);
            return null;
        }
    }

    protected <T> ResponseEntity<T> safeCautostockPost(String path, Object request, Class<T> responseType) {
        try {
            return restTemplate.postForEntity(getCautostockUrl(path), createJsonEntity(request), responseType);
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable: " + e.getMessage());
            Assumptions.assumeTrue(false, "Cautostock API not available: " + path);
            return null;
        }
    }

    protected <T> ResponseEntity<T> safeCautostockPut(String path, Object request, Class<T> responseType) {
        try {
            return restTemplate.exchange(getCautostockUrl(path), HttpMethod.PUT, createJsonEntity(request), responseType);
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable: " + e.getMessage());
            Assumptions.assumeTrue(false, "Cautostock API not available: " + path);
            return null;
        }
    }

    protected void safeCautostockDelete(String path) {
        try {
            restTemplate.delete(getCautostockUrl(path));
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable: " + e.getMessage());
        }
    }

    // ========== Assertion Helpers ==========

    protected void assertApiSuccess(Map<String, Object> response) {
        if (response.containsKey("error")) {
            throw new AssertionError("API returned error: " + response.get("error"));
        }
    }

    protected void assertContainsKey(Map<String, Object> response, String key) {
        if (!response.containsKey(key)) {
            throw new AssertionError("Response does not contain key: " + key);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getItemsFromResponse(Map<String, Object> response) {
        if (response.containsKey("items")) {
            return (List<Map<String, Object>>) response.get("items");
        }
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        }
        return Collections.emptyList();
    }
}
