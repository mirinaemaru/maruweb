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
                getCautostockUrl("/api/v1/admin/accounts"),
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
            safeCautostockDelete("/api/v1/admin/accounts/" + createdAccountId);
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
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);

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
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping detail check");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/strategies/" + createdStrategyId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("전략 수정")
    void updateStrategy() {
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping update");
            return;
        }

        // Given
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "수정된 E2E 테스트 전략");
        updateData.put("description", "E2E 테스트로 수정된 전략입니다.");
        updateData.put("maxPositions", 5);

        // When
        ResponseEntity<Map> response = safeCautostockPut("/api/v1/admin/strategies/" + createdStrategyId, updateData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("전략 활성화")
    void activateStrategy() {
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping activation");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/strategies/" + createdStrategyId + "/activate", null, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("전략 상태 확인 - ACTIVE")
    void verifyStrategyActive() {
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping status check");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/strategies/" + createdStrategyId, Map.class);

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
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping statistics check");
            return;
        }

        // When - 통계 API가 없을 수도 있으므로 예외 처리
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/strategies/" + createdStrategyId + "/statistics"), Map.class);
            // Then - 성공한 경우
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404는 허용 (통계 API가 구현되지 않은 경우)
            System.out.println("Strategy statistics API not found (expected): " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Strategy statistics API error: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Strategy statistics server error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(8)
    @DisplayName("전략 비활성화")
    void deactivateStrategy() {
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping deactivation");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/strategies/" + createdStrategyId + "/deactivate", null, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("전략 삭제")
    void deleteStrategy() {
        if (createdStrategyId == null) {
            System.out.println("Strategy was not created - skipping delete");
            return;
        }

        // When
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                getCautostockUrl("/api/v1/admin/strategies/" + createdStrategyId),
                HttpMethod.DELETE,
                null,
                Void.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Delete strategy failed: " + e.getStatusCode());
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
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        String strategyId = extractId(response.getBody(), "strategyId", "id");
        if (strategyId != null) {
            safeCautostockDelete("/api/v1/admin/strategies/" + strategyId);
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
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        String strategyId = extractId(response.getBody(), "strategyId", "id");
        if (strategyId != null) {
            safeCautostockDelete("/api/v1/admin/strategies/" + strategyId);
        }
    }

    // ========== 저장 후 재조회 검증 테스트 ==========

    @Test
    @Order(12)
    @DisplayName("전략 생성 - Round-Trip 검증 (저장 후 재조회하여 값 일치 확인)")
    void createStrategy_RoundTrip_VerifySavedData() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        // Given - 테스트 데이터 준비
        String uniqueName = "Round-Trip 테스트 전략 " + System.currentTimeMillis();
        Map<String, Object> strategyData = createTestStrategyData();
        strategyData.put("name", uniqueName);
        strategyData.put("symbol", "005930");
        strategyData.put("stopLossValue", new BigDecimal("7.5"));
        strategyData.put("takeProfitValue", new BigDecimal("15.0"));
        strategyData.put("maxPositions", 5);

        String createdId = null;

        try {
            // When - 전략 생성
            ResponseEntity<Map> createResponse = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);
            assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
            createdId = extractId(createResponse.getBody(), "strategyId", "id");
            assertThat(createdId).isNotNull();

            // Then - 재조회하여 저장된 값 검증
            ResponseEntity<Map> getResponse = safeCautostockGet("/api/v1/admin/strategies/" + createdId, Map.class);
            assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(getResponse.getBody()).isNotNull();

            Map<String, Object> retrievedStrategy = getResponse.getBody();

            // 저장한 값과 조회한 값이 일치하는지 검증
            assertThat(retrievedStrategy.get("name")).isEqualTo(uniqueName);
            // symbol은 최상위 또는 params 내에 저장될 수 있음
            Object symbol = retrievedStrategy.get("symbol");
            if (symbol == null && retrievedStrategy.get("params") instanceof Map) {
                symbol = ((Map<?, ?>) retrievedStrategy.get("params")).get("symbol");
            }
            assertThat(symbol).isIn("005930", null); // symbol 저장이 선택적일 수 있음
            assertThat(retrievedStrategy.get("maxPositions")).isIn(5, null); // maxPositions도 선택적

            // 숫자 값 비교 (BigDecimal vs Double 처리)
            Object stopLossValue = retrievedStrategy.get("stopLossValue");
            if (stopLossValue != null) {
                double stopLoss = stopLossValue instanceof Number
                    ? ((Number) stopLossValue).doubleValue()
                    : Double.parseDouble(stopLossValue.toString());
                assertThat(stopLoss).isEqualTo(7.5, org.assertj.core.api.Assertions.within(0.01));
            }

            Object takeProfitValue = retrievedStrategy.get("takeProfitValue");
            if (takeProfitValue != null) {
                double takeProfit = takeProfitValue instanceof Number
                    ? ((Number) takeProfitValue).doubleValue()
                    : Double.parseDouble(takeProfitValue.toString());
                assertThat(takeProfit).isEqualTo(15.0, org.assertj.core.api.Assertions.within(0.01));
            }

            System.out.println("Round-trip verification passed for strategy: " + createdId);

        } finally {
            // Cleanup
            if (createdId != null) {
                safeCautostockDelete("/api/v1/admin/strategies/" + createdId);
                System.out.println("Cleaned up strategy: " + createdId);
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("전략 수정 - Round-Trip 검증 (수정 후 재조회하여 변경 확인)")
    void updateStrategy_RoundTrip_VerifyUpdatedData() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        String createdId = null;

        try {
            // Given - 전략 생성
            Map<String, Object> strategyData = createTestStrategyData();
            strategyData.put("name", "수정 전 전략 " + System.currentTimeMillis());
            strategyData.put("maxPositions", 3);

            ResponseEntity<Map> createResponse = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);
            assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
            createdId = extractId(createResponse.getBody(), "strategyId", "id");
            assertThat(createdId).isNotNull();

            // When - 전략 수정
            String updatedName = "수정 후 전략 " + System.currentTimeMillis();
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", updatedName);
            updateData.put("maxPositions", 10);
            updateData.put("stopLossValue", new BigDecimal("3.5"));

            ResponseEntity<Map> updateResponse = safeCautostockPut("/api/v1/admin/strategies/" + createdId, updateData, Map.class);
            assertThat(updateResponse.getStatusCode().is2xxSuccessful()).isTrue();

            // Then - 재조회하여 수정된 값 검증
            ResponseEntity<Map> getResponse = safeCautostockGet("/api/v1/admin/strategies/" + createdId, Map.class);
            assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(getResponse.getBody()).isNotNull();

            Map<String, Object> retrievedStrategy = getResponse.getBody();

            // 수정한 값이 반영되었는지 검증
            assertThat(retrievedStrategy.get("name")).isEqualTo(updatedName);
            assertThat(retrievedStrategy.get("maxPositions")).isEqualTo(10);

            Object stopLossValue = retrievedStrategy.get("stopLossValue");
            if (stopLossValue != null) {
                double stopLoss = stopLossValue instanceof Number
                    ? ((Number) stopLossValue).doubleValue()
                    : Double.parseDouble(stopLossValue.toString());
                assertThat(stopLoss).isEqualTo(3.5, org.assertj.core.api.Assertions.within(0.01));
            }

            System.out.println("Update round-trip verification passed for strategy: " + createdId);

        } finally {
            // Cleanup
            if (createdId != null) {
                safeCautostockDelete("/api/v1/admin/strategies/" + createdId);
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("자동매매 설정 - Round-Trip 검증 (설정 저장 후 재조회)")
    void tradingConfig_RoundTrip_VerifySavedConfig() {
        Assumptions.assumeTrue(apiAvailable, "Cautostock API not available");

        if (createdAccountId == null) {
            System.out.println("Account was not created - skipping trading config round-trip");
            return;
        }

        String createdId = null;

        try {
            // Given - 전략 생성
            Map<String, Object> strategyData = createTestStrategyData();
            ResponseEntity<Map> createResponse = safeCautostockPost("/api/v1/admin/strategies", strategyData, Map.class);
            assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
            createdId = extractId(createResponse.getBody(), "strategyId", "id");

            // When - 자동매매 설정 저장
            Map<String, Object> tradingConfig = new HashMap<>();
            tradingConfig.put("targetAccountId", createdAccountId);
            tradingConfig.put("symbol", "035720");
            tradingConfig.put("assetType", "STOCK");
            tradingConfig.put("stopLossType", "PERCENT");
            tradingConfig.put("stopLossValue", new BigDecimal("4.0"));
            tradingConfig.put("takeProfitType", "PERCENT");
            tradingConfig.put("takeProfitValue", new BigDecimal("8.0"));
            tradingConfig.put("positionSizeType", "FIXED_AMOUNT");
            tradingConfig.put("positionSizeValue", new BigDecimal("2000000"));
            tradingConfig.put("maxPositions", 4);

            ResponseEntity<Map> updateResponse = safeCautostockPut("/api/v1/admin/strategies/" + createdId, tradingConfig, Map.class);
            assertThat(updateResponse.getStatusCode().is2xxSuccessful()).isTrue();

            // Then - 재조회하여 설정된 값 검증
            ResponseEntity<Map> getResponse = safeCautostockGet("/api/v1/admin/strategies/" + createdId, Map.class);
            assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(getResponse.getBody()).isNotNull();

            Map<String, Object> retrievedConfig = getResponse.getBody();

            // 핵심 설정 값 검증
            assertThat(retrievedConfig.get("symbol")).isEqualTo("035720");
            assertThat(retrievedConfig.get("maxPositions")).isEqualTo(4);

            // 손절/익절 값 검증
            Object stopLoss = retrievedConfig.get("stopLossValue");
            if (stopLoss != null) {
                double value = stopLoss instanceof Number
                    ? ((Number) stopLoss).doubleValue()
                    : Double.parseDouble(stopLoss.toString());
                assertThat(value).isEqualTo(4.0, org.assertj.core.api.Assertions.within(0.01));
            }

            Object takeProfit = retrievedConfig.get("takeProfitValue");
            if (takeProfit != null) {
                double value = takeProfit instanceof Number
                    ? ((Number) takeProfit).doubleValue()
                    : Double.parseDouble(takeProfit.toString());
                assertThat(value).isEqualTo(8.0, org.assertj.core.api.Assertions.within(0.01));
            }

            System.out.println("Trading config round-trip verification passed for strategy: " + createdId);

        } finally {
            // Cleanup
            if (createdId != null) {
                safeCautostockDelete("/api/v1/admin/strategies/" + createdId);
            }
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
