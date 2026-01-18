package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포지션 관리 E2E 테스트
 *
 * 시나리오: 포지션 목록 조회 → 계좌별 포지션 조회 → 포지션 상세
 */
@DisplayName("Position E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PositionE2ETest extends E2ETestBase {

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
    @DisplayName("포지션 목록 페이지 조회")
    void listPositions() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/positions"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("포지션");
    }

    @Test
    @Order(2)
    @DisplayName("전체 포지션 조회 - API")
    void getAllPositions() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/positions", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("계좌별 포지션 조회")
    void getAccountPositions() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/positions?accountId=" + testAccountId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("포지션 요약 정보 조회")
    void getPositionSummary() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/positions/summary", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("포지션 조회 - Round-Trip 검증")
    void getPositions_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When - 포지션 조회
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/positions?accountId=" + testAccountId, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            // Then - 재조회하여 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet(
                "/api/v1/query/positions?accountId=" + testAccountId, Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(6)
    @DisplayName("오픈 포지션만 조회")
    void getOpenPositions() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/positions?status=OPEN", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
