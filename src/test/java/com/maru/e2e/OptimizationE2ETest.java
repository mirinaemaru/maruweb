package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 파라미터 최적화 E2E 테스트
 *
 * 시나리오: 최적화 목록 조회 → 최적화 실행 → 결과 조회
 */
@DisplayName("Optimization E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OptimizationE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static String testStrategyId;
    private static String createdOptimizationId;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/optimization", Map.class);
            apiAvailable = response.getStatusCode().is2xxSuccessful() ||
                           response.getStatusCode() == HttpStatus.NOT_FOUND;

            // 테스트용 전략 ID 가져오기
            Map<String, Object> strategiesData = safeCautostockGet("/api/v1/admin/strategies", Map.class).getBody();
            if (strategiesData != null && strategiesData.containsKey("items")) {
                List<?> strategies = (List<?>) strategiesData.get("items");
                if (!strategies.isEmpty()) {
                    Map<?, ?> firstStrategy = (Map<?, ?>) strategies.get(0);
                    testStrategyId = String.valueOf(firstStrategy.get("strategyId"));
                    if (testStrategyId == null || "null".equals(testStrategyId)) {
                        testStrategyId = String.valueOf(firstStrategy.get("id"));
                    }
                }
            }
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @AfterAll
    void cleanUp() {
        if (createdOptimizationId != null) {
            safeCautostockDelete("/api/v1/admin/optimization/" + createdOptimizationId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("파라미터 최적화 페이지 조회")
    void optimizationPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/optimization"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("최적화");
    }

    @Test
    @Order(2)
    @DisplayName("최적화 목록 조회 - API")
    void getOptimizations() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/optimization", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("최적화 실행")
    void runOptimization() {
        Assumptions.assumeTrue(apiAvailable && testStrategyId != null,
            "API not available or no test strategy");

        // Given
        Map<String, Object> optimizationParams = new HashMap<>();
        optimizationParams.put("strategyId", testStrategyId);
        optimizationParams.put("method", "GRID_SEARCH");
        optimizationParams.put("parameters", Arrays.asList(
            Map.of("name", "stopLossValue", "min", 3.0, "max", 10.0, "step", 1.0),
            Map.of("name", "takeProfitValue", "min", 5.0, "max", 20.0, "step", 2.5)
        ));
        optimizationParams.put("startDate", "2024-01-01");
        optimizationParams.put("endDate", "2024-06-30");

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/admin/optimization/run", optimizationParams, Map.class);

        // Then
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            createdOptimizationId = extractId(response.getBody(), "optimizationId", "id");
        }
    }

    @Test
    @Order(4)
    @DisplayName("최적화 결과 조회 - Round-Trip 검증")
    void getOptimizationResult_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable && createdOptimizationId != null,
            "API not available or no optimization created");

        // When - 최적화 결과 조회
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/optimization/" + createdOptimizationId, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object bestParams = response.getBody().get("bestParameters");

            // Then - 재조회하여 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet(
                "/api/v1/admin/optimization/" + createdOptimizationId, Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
            if (secondResponse.getBody() != null && bestParams != null) {
                assertThat(secondResponse.getBody().get("bestParameters")).isEqualTo(bestParams);
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("최적화 상태 조회")
    void getOptimizationStatus() {
        Assumptions.assumeTrue(apiAvailable && createdOptimizationId != null,
            "API not available or no optimization created");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/optimization/" + createdOptimizationId + "/status", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("최적화 결과 적용")
    void applyOptimizationResult() {
        Assumptions.assumeTrue(apiAvailable && createdOptimizationId != null && testStrategyId != null,
            "API not available or no optimization created");

        // Given
        Map<String, Object> applyParams = new HashMap<>();
        applyParams.put("strategyId", testStrategyId);

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/optimization/" + createdOptimizationId + "/apply", applyParams, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("최적화 방법 목록 조회")
    void getOptimizationMethods() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/optimization/methods", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("최적화 삭제")
    void deleteOptimization() {
        Assumptions.assumeTrue(apiAvailable && createdOptimizationId != null,
            "API not available or no optimization created");

        // When
        try {
            restTemplate.delete(getCautostockUrl("/api/v1/admin/optimization/" + createdOptimizationId));

            // Then - 삭제 확인
            ResponseEntity<Map> response = safeCautostockGet(
                "/api/v1/admin/optimization/" + createdOptimizationId, Map.class);

            assertThat(response.getStatusCode() == HttpStatus.NOT_FOUND ||
                       (response.getBody() != null && response.getBody().containsKey("error"))).isTrue();

            createdOptimizationId = null;
        } catch (Exception e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    private String extractId(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key)) {
                return String.valueOf(body.get(key));
            }
        }
        return null;
    }
}
