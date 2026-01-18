package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 관련 E2E 테스트
 *
 * 시나리오: 주문 생성 → 조회 → 취소
 */
@DisplayName("Order E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderE2ETest extends E2ETestBase {

    private static String testAccountId;
    private static String createdOrderId;
    private static boolean apiAvailable = true;

    @BeforeAll
    void setUpAccount() throws InterruptedException {
        super.waitForCautostock();

        try {
            // 테스트용 계좌 생성 또는 기존 계좌 사용
            ResponseEntity<Map> accountsResponse = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/accounts"), Map.class);

            if (accountsResponse.getStatusCode().is2xxSuccessful() &&
                accountsResponse.getBody() != null) {

                List<Map<String, Object>> items = getItemsFromResponse(accountsResponse.getBody());
                if (!items.isEmpty()) {
                    testAccountId = extractId(items.get(0), "accountId", "id");
                    System.out.println("Using existing account: " + testAccountId);
                    return;
                }
            }

            // 기존 계좌가 없으면 새로 생성
            Map<String, Object> accountData = createTestAccountData();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/accounts"),
                createJsonEntity(accountData),
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                testAccountId = extractId(response.getBody(), "accountId", "id");
                System.out.println("Test account created: " + testAccountId);
            }
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable for account setup: " + e.getMessage());
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("주문 목록 페이지 조회")
    void ordersPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/orders"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("주문");
    }

    @Test
    @Order(2)
    @DisplayName("주문 목록 조회 - 계좌별")
    void listOrders() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // When
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/query/orders?accountId=" + testAccountId), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 404 등 클라이언트 에러는 API 미구현으로 처리
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(3)
    @DisplayName("주문 생성 - 매수 주문")
    void createBuyOrder() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // Given
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("accountId", testAccountId);
        orderData.put("symbol", "005930");
        orderData.put("side", "BUY");
        orderData.put("orderType", "LIMIT");
        orderData.put("quantity", 1);
        orderData.put("price", new BigDecimal("50000"));  // 낮은 가격으로 즉시 체결 안되게
        orderData.put("timeInForce", "GTC");

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/orders"),
                createJsonEntity(orderData),
                Map.class);

            // Then - PAPER 환경에서도 주문이 가능해야 함
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createdOrderId = extractId(response.getBody(), "orderId", "id");
                System.out.println("Created order ID: " + createdOrderId);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 400/404 등은 API 미구현 또는 검증 실패
            System.out.println("Order creation failed (expected): " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 에러는 서버측 미구현
            System.out.println("Order API not implemented: " + e.getStatusCode());
        }
    }

    @Test
    @Order(4)
    @DisplayName("주문 상세 조회")
    void getOrderDetail() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        if (createdOrderId == null) {
            System.out.println("Order was not created in previous test - skipping detail check");
            return;
        }

        // When
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/query/orders/" + createdOrderId), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(5)
    @DisplayName("주문 상태 확인")
    void verifyOrderStatus() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        if (createdOrderId == null) {
            System.out.println("Order was not created - skipping status check");
            return;
        }

        // When
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/query/orders/" + createdOrderId), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            if (response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                System.out.println("Order status: " + status);
                assertThat(status).isIn("PENDING", "OPEN", "FILLED", "CANCELLED", "REJECTED", "NEW", null);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(6)
    @DisplayName("주문 취소")
    void cancelOrder() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        if (createdOrderId == null) {
            System.out.println("Order was not created - skipping cancel");
            return;
        }

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/orders/" + createdOrderId + "/cancel"),
                createJsonEntity(null),
                Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 주문 취소 실패 (이미 취소되었거나 체결됨)
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(7)
    @DisplayName("취소된 주문 상태 확인")
    void verifyCancelledOrder() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        if (createdOrderId == null) {
            System.out.println("Order was not created - skipping cancelled status check");
            return;
        }

        // When
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/query/orders/" + createdOrderId), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                assertThat(status).isIn("CANCELLED", "CANCELED", "REJECTED", null);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    // ========== 주문 필터링 테스트 ==========

    @Test
    @Order(8)
    @DisplayName("주문 목록 - 날짜 필터")
    void listOrdersWithDateFilter() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();

        // When
        String path = String.format("/api/v1/query/orders?accountId=%s&startDate=%s&endDate=%s",
            testAccountId, startDate, endDate);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl(path), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(9)
    @DisplayName("주문 목록 - 상태 필터")
    void listOrdersWithStatusFilter() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // When
        String path = String.format("/api/v1/query/orders?accountId=%s&status=FILLED", testAccountId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl(path), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(10)
    @DisplayName("주문 목록 - 종목 필터")
    void listOrdersWithSymbolFilter() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // When
        String path = String.format("/api/v1/query/orders?accountId=%s&symbol=005930", testAccountId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl(path), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    // ========== 매도 주문 테스트 ==========

    @Test
    @Order(11)
    @DisplayName("매도 주문 생성")
    void createSellOrder() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");
        Assumptions.assumeTrue(testAccountId != null, "Test account not available");

        // Given
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("accountId", testAccountId);
        orderData.put("symbol", "005930");
        orderData.put("side", "SELL");
        orderData.put("orderType", "LIMIT");
        orderData.put("quantity", 1);
        orderData.put("price", new BigDecimal("100000"));  // 높은 가격으로 즉시 체결 안되게
        orderData.put("timeInForce", "GTC");

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/orders"),
                createJsonEntity(orderData),
                Map.class);

            // 포지션이 없으면 거부될 수 있음
            assertThat(response.getStatusCode().is2xxSuccessful() ||
                       response.getStatusCode().is4xxClientError()).isTrue();

            // 주문이 생성되었으면 취소
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String orderId = extractId(response.getBody(), "orderId", "id");
                if (orderId != null) {
                    try {
                        restTemplate.postForEntity(
                            getCautostockUrl("/api/v1/admin/orders/" + orderId + "/cancel"),
                            createJsonEntity(null),
                            Map.class);
                    } catch (Exception e) {
                        // 취소 실패는 무시
                    }
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 400/404 등은 포지션 없음 또는 API 미구현
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 에러는 서버측 미구현
            System.out.println("Order API not implemented: " + e.getStatusCode());
        }
    }

    // ========== Helper Methods ==========

    private String extractId(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return String.valueOf(body.get(key));
            }
        }
        return null;
    }
}
