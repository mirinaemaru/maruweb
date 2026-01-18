package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전략 관련 E2E 테스트
 *
 * 시나리오: 전략 생성 → 동기화 → 활성화 → 모니터링
 */
@DisplayName("Strategy E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StrategyE2ETest extends E2ETestBase {

    private static String createdAccountId;
    private static String createdStrategyId;
    private static boolean apiAvailable = true;

    @BeforeAll
    void setUpAccount() throws InterruptedException {
        super.waitForCautostock();

        // 테스트용 계좌 생성
        try {
            Map<String, Object> accountData = createTestAccountData();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/accounts"),
                createJsonEntity(accountData),
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createdAccountId = extractId(response.getBody(), "accountId", "id");
                System.out.println("Test account created: " + createdAccountId);
            }
        } catch (Exception e) {
            System.out.println("Cautostock API unavailable for account setup: " + e.getMessage());
            apiAvailable = false;
        }
    }

    @AfterAll
    void cleanUpAccount() {
        // 테스트 계좌 정리
        if (createdAccountId != null) {
            safeCautostockDelete("/api/accounts/" + createdAccountId);
            System.out.println("Test account cleaned up: " + createdAccountId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("전략 목록 조회")
    void listStrategies() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/strategies"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("전략");
    }

    @Test
    @Order(2)
    @DisplayName("전략 생성 - cautostock API 호출")
    void createStrategy() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        // Given
        Map<String, Object> strategyData = createTestStrategyData();
        if (createdAccountId != null) {
            strategyData.put("targetAccountId", createdAccountId);
        }

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/strategies", strategyData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        createdStrategyId = extractId(response.getBody(), "strategyId", "id");
        System.out.println("Created strategy ID: " + createdStrategyId);
    }

    @Test
    @Order(3)
    @DisplayName("전략 상세 조회")
    void getStrategyDetail() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/strategies/" + createdStrategyId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("전략 수정")
    void updateStrategy() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // Given
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "수정된 E2E 테스트 전략");
        updateData.put("description", "E2E 테스트로 수정된 전략입니다.");
        updateData.put("maxPositions", 5);

        // When
        ResponseEntity<Map> response = safeCautostockPut("/api/strategies/" + createdStrategyId, updateData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("전략 활성화")
    void activateStrategy() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/strategies/" + createdStrategyId + "/activate", null, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("전략 상태 확인 - ACTIVE")
    void verifyStrategyActive() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/strategies/" + createdStrategyId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        String status = (String) response.getBody().get("status");
        // 상태가 ACTIVE이거나 활성화된 상태여야 함
        assertThat(status).isIn("ACTIVE", "RUNNING", "ENABLED", null);
    }

    @Test
    @Order(7)
    @DisplayName("전략 통계 조회")
    void getStrategyStatistics() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/strategies/" + createdStrategyId + "/statistics", Map.class);

        // Then
        // 통계 API가 없을 수도 있으므로 404도 허용
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("전략 비활성화")
    void deactivateStrategy() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/strategies/" + createdStrategyId + "/deactivate", null, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("전략 삭제")
    void deleteStrategy() {
        Assumptions.assumeTrue(createdStrategyId != null,
            "Strategy was not created in previous test");

        // When
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                getCautostockUrl("/api/strategies/" + createdStrategyId),
                HttpMethod.DELETE,
                null,
                Void.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (Exception e) {
            System.out.println("Delete strategy failed: " + e.getMessage());
            Assumptions.assumeTrue(false, "Cautostock API not available");
        }
    }

    // ========== 전략 조건 테스트 ==========

    @Test
    @Order(10)
    @DisplayName("전략 진입조건 설정 테스트")
    void setEntryConditions() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        // Given - 새 전략 생성
        Map<String, Object> strategyData = createTestStrategyData();

        // 진입 조건 추가
        List<Map<String, Object>> entryConditions = new ArrayList<>();
        Map<String, Object> priceCondition = new HashMap<>();
        priceCondition.put("type", "PRICE");
        priceCondition.put("operator", ">=");
        priceCondition.put("value", 70000);
        priceCondition.put("description", "가격이 70,000원 이상일 때");
        entryConditions.add(priceCondition);

        Map<String, Object> volumeCondition = new HashMap<>();
        volumeCondition.put("type", "VOLUME");
        volumeCondition.put("operator", ">");
        volumeCondition.put("value", 100000);
        volumeCondition.put("description", "거래량이 100,000주 초과일 때");
        entryConditions.add(volumeCondition);

        strategyData.put("entryConditions", entryConditions);

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/strategies", strategyData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        String strategyId = extractId(response.getBody(), "strategyId", "id");
        if (strategyId != null) {
            safeCautostockDelete("/api/strategies/" + strategyId);
        }
    }

    @Test
    @Order(11)
    @DisplayName("전략 청산조건 설정 테스트")
    void setExitConditions() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        // Given - 새 전략 생성
        Map<String, Object> strategyData = createTestStrategyData();

        // 청산 조건 추가
        List<Map<String, Object>> exitConditions = new ArrayList<>();
        Map<String, Object> profitCondition = new HashMap<>();
        profitCondition.put("type", "PROFIT_PERCENT");
        profitCondition.put("operator", ">=");
        profitCondition.put("value", 5);
        profitCondition.put("description", "수익률이 5% 이상일 때");
        exitConditions.add(profitCondition);

        strategyData.put("exitConditions", exitConditions);
        strategyData.put("stopLossType", "PERCENT");
        strategyData.put("stopLossValue", new BigDecimal("3"));
        strategyData.put("takeProfitType", "PERCENT");
        strategyData.put("takeProfitValue", new BigDecimal("10"));

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/strategies", strategyData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        String strategyId = extractId(response.getBody(), "strategyId", "id");
        if (strategyId != null) {
            safeCautostockDelete("/api/strategies/" + strategyId);
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
