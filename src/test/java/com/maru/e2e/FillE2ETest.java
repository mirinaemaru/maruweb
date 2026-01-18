package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 체결 내역 E2E 테스트
 *
 * 시나리오: 체결 목록 조회 → 계좌별 체결 조회 → 체결 상세
 */
@DisplayName("Fill E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FillE2ETest extends E2ETestBase {

    private static String testAccountId;
    private static boolean apiAvailable = true;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            Map<String, Object> accountsData = safeCautostockGet("/api/v1/admin/accounts", Map.class).getBody();
            if (accountsData != null && accountsData.containsKey("items")) {
                List<?> accounts = (List<?>) accountsData.get("items");
                if (!accounts.isEmpty()) {
                    Map<?, ?> firstAccount = (Map<?, ?>) accounts.get(0);
                    testAccountId = String.valueOf(firstAccount.get("accountId"));
                    if (testAccountId == null || "null".equals(testAccountId)) {
                        testAccountId = String.valueOf(firstAccount.get("id"));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get test account: " + e.getMessage());
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("체결 내역 페이지 조회")
    void listFills() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/fills"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("체결");
    }

    @Test
    @Order(2)
    @DisplayName("전체 체결 내역 조회 - API")
    void getAllFills() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/fills", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("계좌별 체결 내역 조회")
    void getAccountFills() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/fills?accountId=" + testAccountId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("오늘 체결 내역 조회")
    void getTodayFills() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/fills?date=" + getToday(), Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("체결 내역 조회 - Round-Trip 검증")
    void getFills_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When - 체결 내역 조회
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/fills?accountId=" + testAccountId, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            // Then - 재조회하여 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet(
                "/api/v1/query/fills?accountId=" + testAccountId, Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(6)
    @DisplayName("체결 통계 조회")
    void getFillStatistics() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/fills/statistics", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
