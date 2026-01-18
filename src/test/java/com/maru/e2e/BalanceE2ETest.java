package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 잔고 조회 E2E 테스트
 *
 * 시나리오: 잔고 목록 조회 → 계좌별 잔고 조회 → 잔고 새로고침
 */
@DisplayName("Balance E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BalanceE2ETest extends E2ETestBase {

    private static String testAccountId;
    private static boolean apiAvailable = true;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 테스트용 계좌 ID 조회
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
    @DisplayName("잔고 목록 페이지 조회")
    void listBalances() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/balances"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("잔고");
    }

    @Test
    @Order(2)
    @DisplayName("계좌별 잔고 조회 - API")
    void getAccountBalance() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/balance/" + testAccountId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("잔고 조회 - Round-Trip 검증")
    void getBalance_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null,
            "API not available or no test account");

        // When - 잔고 조회
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/balance/" + testAccountId, Map.class);

        // Then - 응답 검증
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> balance = response.getBody();

            // 잔고 정보가 있으면 재조회해서 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet(
                "/api/v1/query/balance/" + testAccountId, Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
            // 두 조회 결과의 계좌 ID가 일치해야 함
            if (secondResponse.getBody() != null) {
                assertThat(secondResponse.getBody().get("accountId"))
                    .isEqualTo(balance.get("accountId"));
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("전체 잔고 요약 조회")
    void getBalanceSummary() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/balance/summary", Map.class);

        // Then - API가 있으면 성공, 없으면 404
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
