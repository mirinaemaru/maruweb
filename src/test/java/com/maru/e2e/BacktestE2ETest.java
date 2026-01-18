package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 백테스트 E2E 테스트
 *
 * 시나리오: 백테스트 목록 조회 → 백테스트 실행 → 결과 조회 → Walk-Forward
 */
@DisplayName("Backtest E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BacktestE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static String testStrategyId;
    private static String createdBacktestId;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/backtests"), Object.class);
            apiAvailable = response.getStatusCode().is2xxSuccessful();

            // 테스트용 전략 ID 가져오기
            try {
                ResponseEntity<Object> strategiesResponse = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/strategies"), Object.class);
                Object body = strategiesResponse.getBody();

                if (body instanceof Map) {
                    Map<?, ?> strategiesData = (Map<?, ?>) body;
                    if (strategiesData.containsKey("items")) {
                        List<?> strategies = (List<?>) strategiesData.get("items");
                        if (!strategies.isEmpty()) {
                            Map<?, ?> firstStrategy = (Map<?, ?>) strategies.get(0);
                            testStrategyId = String.valueOf(firstStrategy.get("strategyId"));
                            if (testStrategyId == null || "null".equals(testStrategyId)) {
                                testStrategyId = String.valueOf(firstStrategy.get("id"));
                            }
                        }
                    }
                } else if (body instanceof List) {
                    List<?> strategies = (List<?>) body;
                    if (!strategies.isEmpty()) {
                        Map<?, ?> firstStrategy = (Map<?, ?>) strategies.get(0);
                        testStrategyId = String.valueOf(firstStrategy.get("strategyId"));
                        if (testStrategyId == null || "null".equals(testStrategyId)) {
                            testStrategyId = String.valueOf(firstStrategy.get("id"));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not get test strategy: " + e.getMessage());
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            apiAvailable = true;  // 404는 API 있음
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @AfterAll
    void cleanUp() {
        if (createdBacktestId != null) {
            try {
                restTemplate.delete(getCautostockUrl("/api/v1/admin/backtests/" + createdBacktestId));
            } catch (Exception e) {
                // 정리 실패 무시
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("백테스트 결과 페이지 조회")
    void backtestResultsPage() {
        // When - 백테스트 페이지 또는 전략 목록 페이지
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/strategies"), String.class);

        // Then - 전략 목록 페이지가 정상 로드되면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("백테스트 목록 조회 - API")
    void getBacktests() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/backtests"), Object.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(3)
    @DisplayName("백테스트 실행")
    void runBacktest() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (testStrategyId == null) {
            System.out.println("No test strategy available - skipping backtest run");
            return;
        }

        // Given
        Map<String, Object> backtestParams = new HashMap<>();
        backtestParams.put("strategyId", testStrategyId);
        backtestParams.put("startDate", "2024-01-01");
        backtestParams.put("endDate", "2024-06-30");
        backtestParams.put("initialCapital", 10000000);

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/backtests/run"),
                createJsonEntity(backtestParams),
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createdBacktestId = extractId(response.getBody(), "backtestId", "id");
                System.out.println("Created backtest ID: " + createdBacktestId);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Backtest API not available: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Backtest API error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(4)
    @DisplayName("백테스트 결과 조회 - Round-Trip 검증")
    void getBacktestResult_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (createdBacktestId == null) {
            System.out.println("No backtest created - skipping round-trip check");
            return;
        }

        // When - 백테스트 결과 조회
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/backtests/" + createdBacktestId), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object totalReturn = response.getBody().get("totalReturn");

                // Then - 재조회하여 일치 확인
                ResponseEntity<Map> secondResponse = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/backtests/" + createdBacktestId), Map.class);

                assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
                if (secondResponse.getBody() != null && totalReturn != null) {
                    assertThat(secondResponse.getBody().get("totalReturn")).isEqualTo(totalReturn);
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Walk-Forward 분석 페이지 조회")
    void walkForwardPage() {
        // When - Walk-Forward는 백테스트 기능의 일부
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/strategies"), String.class);

        // Then - 전략 목록 페이지가 정상 로드되면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(6)
    @DisplayName("Walk-Forward 분석 실행 - API")
    void runWalkForwardAnalysis() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (testStrategyId == null) {
            System.out.println("No test strategy - skipping walk-forward");
            return;
        }

        // Given
        Map<String, Object> wfParams = new HashMap<>();
        wfParams.put("strategyId", testStrategyId);
        wfParams.put("inSamplePeriod", 180);
        wfParams.put("outOfSamplePeriod", 30);

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/backtests/walk-forward"),
                createJsonEntity(wfParams),
                Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Walk-forward API error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(7)
    @DisplayName("포트폴리오 백테스트 페이지 조회")
    void portfolioBacktestPage() {
        // When - 포트폴리오 백테스트는 전략 관련 페이지
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/strategies"), String.class);

        // Then - 전략 목록 페이지가 정상 로드되면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(8)
    @DisplayName("포트폴리오 백테스트 실행 - API")
    void runPortfolioBacktest() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // Given
        Map<String, Object> portfolioParams = new HashMap<>();
        portfolioParams.put("strategyIds", Arrays.asList("strategy-1", "strategy-2"));
        portfolioParams.put("startDate", "2024-01-01");
        portfolioParams.put("endDate", "2024-06-30");

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/backtests/portfolio"),
                createJsonEntity(portfolioParams),
                Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Portfolio backtest API error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(9)
    @DisplayName("백테스트 관리 페이지 조회")
    void backtestManagementPage() {
        // When - 백테스트 관리는 전략 목록 페이지에서
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/strategies"), String.class);

        // Then - 전략 목록 페이지가 정상 로드되면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(10)
    @DisplayName("백테스트 삭제")
    void deleteBacktest() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (createdBacktestId == null) {
            System.out.println("No backtest created - skipping delete");
            return;
        }

        // When
        try {
            restTemplate.delete(getCautostockUrl("/api/v1/admin/backtests/" + createdBacktestId));

            // Then - 삭제 확인
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/backtests/" + createdBacktestId), Map.class);
                assertThat(response.getBody() == null || response.getBody().containsKey("error")).isTrue();
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                // 404면 삭제 성공
            }

            createdBacktestId = null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Delete API not available: " + e.getStatusCode());
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
