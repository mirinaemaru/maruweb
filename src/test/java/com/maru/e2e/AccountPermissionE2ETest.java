package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Account Permission E2E 테스트
 *
 * 시나리오: 계좌 권한 조회 → 권한 업데이트 → 데이터 검증
 *
 * 테스트 실행 전 cautostock 서버 시작 필요:
 * cd /Users/changsupark/projects/cautostock && ./gradlew bootRun
 */
@DisplayName("Account Permission E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountPermissionE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static String testAccountId = null;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ResponseEntity<Map> response = safeCautostockGet("/health", Map.class);
            apiAvailable = response != null && response.getStatusCode().is2xxSuccessful();

            // 테스트용 계좌 찾기
            if (apiAvailable) {
                ResponseEntity<Map> accountsResponse = safeCautostockGet("/api/v1/admin/accounts", Map.class);
                if (accountsResponse != null && accountsResponse.getBody() != null) {
                    Object accountsObj = accountsResponse.getBody().get("items");
                    if (accountsObj instanceof List && !((List<?>) accountsObj).isEmpty()) {
                        Map<String, Object> firstAccount = (Map<String, Object>) ((List<?>) accountsObj).get(0);
                        testAccountId = (String) firstAccount.get("accountId");
                        System.out.println("Using test account: " + testAccountId);
                    }
                }
            }
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    // ==================== Permission Retrieval Tests ====================

    @Test
    @Order(1)
    @DisplayName("계좌 권한 조회 - 성공")
    void getAccountPermission_Success() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions", Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> permission = response.getBody();
            assertThat(permission).containsKey("accountId");
            assertThat(permission).containsKey("tradeBuy");
            assertThat(permission).containsKey("tradeSell");
            assertThat(permission).containsKey("autoTrade");
            assertThat(permission).containsKey("manualTrade");
            assertThat(permission).containsKey("paperOnly");
            assertThat(permission).containsKey("updatedAt");
        }
    }

    @Test
    @Order(2)
    @DisplayName("계좌 권한 조회 - 필수 필드 검증")
    void getAccountPermission_RequiredFields() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions", Map.class);

        // Then
        assertThat(response).isNotNull();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> permission = response.getBody();

            // 모든 필드가 Boolean 타입인지 확인
            assertThat(permission.get("tradeBuy")).isInstanceOf(Boolean.class);
            assertThat(permission.get("tradeSell")).isInstanceOf(Boolean.class);
            assertThat(permission.get("autoTrade")).isInstanceOf(Boolean.class);
            assertThat(permission.get("manualTrade")).isInstanceOf(Boolean.class);
            assertThat(permission.get("paperOnly")).isInstanceOf(Boolean.class);

            // updatedAt 필드 존재 확인
            assertThat(permission.get("updatedAt")).isNotNull();
        }
    }

    @Test
    @Order(3)
    @DisplayName("계좌 권한 조회 - 미존재 계좌")
    void getAccountPermission_InvalidAccount() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        String invalidAccountId = "invalid_account_id_12345";
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/accounts/" + invalidAccountId + "/permissions", Map.class);

        // Then - 404 에러 또는 null 응답
        if (response != null) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== Permission Update Tests ====================

    @Test
    @Order(4)
    @DisplayName("계좌 권한 업데이트 - 전체 활성화")
    void updateAccountPermission_EnableAll() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", true);
        permissionData.put("tradeSell", true);
        permissionData.put("autoTrade", true);
        permissionData.put("manualTrade", true);
        permissionData.put("paperOnly", true);

        // When
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> permission = response.getBody();
            assertThat(permission.get("tradeBuy")).isEqualTo(true);
            assertThat(permission.get("tradeSell")).isEqualTo(true);
            assertThat(permission.get("autoTrade")).isEqualTo(true);
            assertThat(permission.get("manualTrade")).isEqualTo(true);
            assertThat(permission.get("paperOnly")).isEqualTo(true);
        }
    }

    @Test
    @Order(5)
    @DisplayName("계좌 권한 업데이트 - 부분 활성화")
    void updateAccountPermission_PartialEnable() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", true);
        permissionData.put("tradeSell", false);
        permissionData.put("autoTrade", false);
        permissionData.put("manualTrade", true);
        permissionData.put("paperOnly", true);

        // When
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> permission = response.getBody();
            assertThat(permission.get("tradeBuy")).isEqualTo(true);
            assertThat(permission.get("tradeSell")).isEqualTo(false);
            assertThat(permission.get("autoTrade")).isEqualTo(false);
            assertThat(permission.get("manualTrade")).isEqualTo(true);
            assertThat(permission.get("paperOnly")).isEqualTo(true);
        }
    }

    @Test
    @Order(6)
    @DisplayName("계좌 권한 업데이트 - LIVE 환경 활성화")
    void updateAccountPermission_EnableLive() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", true);
        permissionData.put("tradeSell", true);
        permissionData.put("autoTrade", false);
        permissionData.put("manualTrade", true);
        permissionData.put("paperOnly", false); // LIVE 활성화

        // When
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> permission = response.getBody();
            assertThat(permission.get("paperOnly")).isEqualTo(false);
        }
    }

    @Test
    @Order(7)
    @DisplayName("계좌 권한 업데이트 - 전체 비활성화")
    void updateAccountPermission_DisableAll() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", false);
        permissionData.put("tradeSell", false);
        permissionData.put("autoTrade", false);
        permissionData.put("manualTrade", false);
        permissionData.put("paperOnly", true);

        // When
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> permission = response.getBody();
            assertThat(permission.get("tradeBuy")).isEqualTo(false);
            assertThat(permission.get("tradeSell")).isEqualTo(false);
            assertThat(permission.get("autoTrade")).isEqualTo(false);
            assertThat(permission.get("manualTrade")).isEqualTo(false);
            assertThat(permission.get("paperOnly")).isEqualTo(true);
        }
    }

    @Test
    @Order(8)
    @DisplayName("계좌 권한 업데이트 - 필수 필드 누락")
    void updateAccountPermission_MissingField() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given - tradeSell 필드 누락
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", true);
        // tradeSell 누락

        // When
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);

        // Then - 400 Bad Request 예상
        if (response != null) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ==================== Round-Trip Tests ====================

    @Test
    @Order(9)
    @DisplayName("계좌 권한 - Round-Trip 검증")
    void accountPermission_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given - 원본 권한 설정
        Map<String, Object> originalPermission = new HashMap<>();
        originalPermission.put("tradeBuy", true);
        originalPermission.put("tradeSell", false);
        originalPermission.put("autoTrade", true);
        originalPermission.put("manualTrade", false);
        originalPermission.put("paperOnly", true);

        // When - 업데이트
        ResponseEntity<Map> updateResponse = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            originalPermission,
            Map.class);

        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // Then - 다시 조회하여 일치 확인
        ResponseEntity<Map> getResponse = safeCautostockGet(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions", Map.class);

        assertThat(getResponse).isNotNull();
        assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();

        if (getResponse.getBody() != null) {
            Map<String, Object> retrievedPermission = getResponse.getBody();
            assertThat(retrievedPermission.get("tradeBuy")).isEqualTo(true);
            assertThat(retrievedPermission.get("tradeSell")).isEqualTo(false);
            assertThat(retrievedPermission.get("autoTrade")).isEqualTo(true);
            assertThat(retrievedPermission.get("manualTrade")).isEqualTo(false);
            assertThat(retrievedPermission.get("paperOnly")).isEqualTo(true);
        }
    }

    @Test
    @Order(10)
    @DisplayName("계좌 권한 - 연속 업데이트 검증")
    void accountPermission_ConsecutiveUpdates() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // First Update
        Map<String, Object> firstPermission = new HashMap<>();
        firstPermission.put("tradeBuy", true);
        firstPermission.put("tradeSell", true);
        firstPermission.put("autoTrade", false);
        firstPermission.put("manualTrade", true);
        firstPermission.put("paperOnly", true);

        ResponseEntity<Map> firstResponse = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            firstPermission,
            Map.class);
        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // Second Update
        Map<String, Object> secondPermission = new HashMap<>();
        secondPermission.put("tradeBuy", false);
        secondPermission.put("tradeSell", false);
        secondPermission.put("autoTrade", true);
        secondPermission.put("manualTrade", false);
        secondPermission.put("paperOnly", false);

        ResponseEntity<Map> secondResponse = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            secondPermission,
            Map.class);

        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify second update
        if (secondResponse.getBody() != null) {
            Map<String, Object> finalPermission = secondResponse.getBody();
            assertThat(finalPermission.get("tradeBuy")).isEqualTo(false);
            assertThat(finalPermission.get("tradeSell")).isEqualTo(false);
            assertThat(finalPermission.get("autoTrade")).isEqualTo(true);
            assertThat(finalPermission.get("manualTrade")).isEqualTo(false);
            assertThat(finalPermission.get("paperOnly")).isEqualTo(false);
        }
    }

    // ==================== Performance Tests ====================

    @Test
    @Order(11)
    @DisplayName("계좌 권한 조회 - 응답 시간 검증")
    void getAccountPermission_ResponseTime() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions", Map.class);
        long endTime = System.currentTimeMillis();

        // Then - 2초 이내 응답
        long responseTime = endTime - startTime;
        assertThat(responseTime).isLessThan(2000L);

        if (response != null) {
            System.out.println("Account permission API response time: " + responseTime + "ms");
        }
    }

    @Test
    @Order(12)
    @DisplayName("계좌 권한 업데이트 - 응답 시간 검증")
    void updateAccountPermission_ResponseTime() {
        Assumptions.assumeTrue(apiAvailable && testAccountId != null, "API not available or no test account");

        // Given
        Map<String, Object> permissionData = new HashMap<>();
        permissionData.put("tradeBuy", true);
        permissionData.put("tradeSell", true);
        permissionData.put("autoTrade", false);
        permissionData.put("manualTrade", true);
        permissionData.put("paperOnly", true);

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = safeCautostockPut(
            "/api/v1/admin/accounts/" + testAccountId + "/permissions",
            permissionData,
            Map.class);
        long endTime = System.currentTimeMillis();

        // Then - 3초 이내 응답
        long responseTime = endTime - startTime;
        assertThat(responseTime).isLessThan(3000L);

        if (response != null) {
            System.out.println("Account permission update API response time: " + responseTime + "ms");
        }
    }
}
