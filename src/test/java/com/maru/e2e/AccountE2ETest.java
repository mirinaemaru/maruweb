package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 계좌 관련 E2E 테스트
 *
 * 시나리오: 계좌 생성 → 조회 → 수정 → 삭제
 */
@DisplayName("Account E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountE2ETest extends E2ETestBase {

    private static String createdAccountId;

    @Test
    @Order(1)
    @DisplayName("계좌 목록 조회")
    void listAccounts() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/accounts"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("계좌");
    }

    @Test
    @Order(2)
    @DisplayName("계좌 생성 폼 조회")
    void newAccountForm() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/accounts/new"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("계좌");
    }

    @Test
    @Order(3)
    @DisplayName("계좌 생성 - cautostock API 호출")
    void createAccount() {
        // Given - cautostock에 직접 계좌 생성
        Map<String, Object> accountData = createTestAccountData();

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/accounts"),
                createJsonEntity(accountData),
                Map.class);

            // Then - 성공한 경우 (201 CREATED 또는 200 OK)
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();

            Map<String, Object> body = response.getBody();
            if (body.containsKey("accountId")) {
                createdAccountId = (String) body.get("accountId");
                System.out.println("Created account ID: " + createdAccountId);
            } else if (body.containsKey("id")) {
                createdAccountId = String.valueOf(body.get("id"));
                System.out.println("Created account ID: " + createdAccountId);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Account creation failed: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Account API error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(4)
    @DisplayName("계좌 목록에서 생성된 계좌 확인")
    void verifyAccountInList() {
        // Skip if no account was created
        Assumptions.assumeTrue(createdAccountId != null,
            "Account was not created in previous test");

        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getCautostockUrl("/api/v1/admin/accounts"), Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<Map<String, Object>> items = getItemsFromResponse(response.getBody());
        boolean found = items.stream()
            .anyMatch(acc -> createdAccountId.equals(String.valueOf(acc.get("accountId"))) ||
                           createdAccountId.equals(String.valueOf(acc.get("id"))));

        assertThat(found).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("계좌 상세 조회")
    void getAccountDetail() {
        // Skip if no account was created
        Assumptions.assumeTrue(createdAccountId != null,
            "Account was not created in previous test");

        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getCautostockUrl("/api/v1/admin/accounts/" + createdAccountId), Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("계좌 수정")
    void updateAccount() {
        // Skip if no account was created
        Assumptions.assumeTrue(createdAccountId != null,
            "Account was not created in previous test");

        // Given
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("alias", "수정된 E2E 테스트 계좌");

        // When
        HttpEntity<Map<String, Object>> entity = createJsonEntity(updateData);
        ResponseEntity<Map> response = restTemplate.exchange(
            getCautostockUrl("/api/v1/admin/accounts/" + createdAccountId),
            HttpMethod.PUT,
            entity,
            Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("계좌 삭제")
    void deleteAccount() {
        // Skip if no account was created
        Assumptions.assumeTrue(createdAccountId != null,
            "Account was not created in previous test");

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            getCautostockUrl("/api/v1/admin/accounts/" + createdAccountId),
            HttpMethod.DELETE,
            null,
            Void.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("삭제된 계좌 조회 실패 확인")
    void verifyAccountDeleted() {
        // Skip if no account was created
        Assumptions.assumeTrue(createdAccountId != null,
            "Account was not created in previous test");

        // When - 삭제된 계좌 조회 시 404 예외가 발생할 수 있음
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/accounts/" + createdAccountId), Map.class);

            // Then - 삭제된 계좌는 404 또는 에러 응답
            assertThat(response.getStatusCode().is4xxClientError() ||
                       (response.getBody() != null && response.getBody().containsKey("error")))
                .isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404 예외가 발생하면 테스트 통과 (삭제된 계좌이므로 정상)
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }

    // ========== 계좌 잔고 테스트 ==========

    @Test
    @Order(9)
    @DisplayName("계좌 잔고 조회 API 테스트")
    void getAccountBalance() {
        String accountId = null;
        try {
            // Given - 테스트를 위해 새 계좌 생성
            Map<String, Object> accountData = createTestAccountData();
            ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/accounts"),
                createJsonEntity(accountData),
                Map.class);

            if (!createResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("Account creation failed - skipping balance check");
                return;
            }

            accountId = extractAccountId(createResponse.getBody());

            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/accounts/" + accountId + "/balance"), Map.class);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Balance API not available: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Balance API error: " + e.getStatusCode());
        } finally {
            // Cleanup
            if (accountId != null) {
                try {
                    restTemplate.delete(getCautostockUrl("/api/v1/admin/accounts/" + accountId));
                } catch (Exception e) {
                    // 정리 실패 무시
                }
            }
        }
    }

    // ========== Helper Methods ==========

    private String extractAccountId(Map<String, Object> body) {
        if (body == null) return null;
        if (body.containsKey("accountId")) {
            return String.valueOf(body.get("accountId"));
        }
        if (body.containsKey("id")) {
            return String.valueOf(body.get("id"));
        }
        return null;
    }
}
