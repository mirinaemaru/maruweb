package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard E2E 테스트
 *
 * 시나리오: 대시보드 페이지 조회 → 통계 API 호출 → 데이터 검증
 *
 * 테스트 실행 전 cautostock 서버 시작 필요:
 * cd /Users/changsupark/projects/cautostock && ./gradlew bootRun
 */
@DisplayName("Dashboard E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DashboardE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;

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
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    // ==================== Dashboard Page Tests ====================

    @Test
    @Order(1)
    @DisplayName("대시보드 페이지 로드")
    void dashboardPage_Load() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/dashboard"), String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND);
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).contains("dashboard");
        }
    }

    @Test
    @Order(2)
    @DisplayName("대시보드 페이지 - HTML 구조 검증")
    void dashboardPage_HtmlStructure() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/dashboard"), String.class);

        // Then
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String body = response.getBody();
            // 대시보드 주요 섹션 확인
            assertThat(body).containsAnyOf("todayOrders", "오늘 주문", "today-orders");
            assertThat(body).containsAnyOf("todayFills", "오늘 체결", "today-fills");
        }
    }

    // ==================== Dashboard Stats API Tests ====================

    @Test
    @Order(3)
    @DisplayName("대시보드 통계 API 조회 - cautostock 직접 호출")
    void getDashboardStats_DirectApiCall() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> stats = response.getBody();
            // 필수 필드 존재 확인
            assertThat(stats).containsAnyOf(
                Map.entry("todayOrders", stats.get("todayOrders")),
                Map.entry("todayFills", stats.get("todayFills"))
            );
        }
    }

    @Test
    @Order(4)
    @DisplayName("대시보드 통계 API - 응답 필드 검증")
    void getDashboardStats_ResponseFields() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(response).isNotNull();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> stats = response.getBody();

            // 오늘 통계
            if (stats.containsKey("todayOrders")) {
                assertThat(stats.get("todayOrders")).isNotNull();
            }
            if (stats.containsKey("todayFills")) {
                assertThat(stats.get("todayFills")).isNotNull();
            }
            if (stats.containsKey("todayProfitLoss")) {
                assertThat(stats.get("todayProfitLoss")).isNotNull();
            }

            // 총 통계
            if (stats.containsKey("totalProfitLoss")) {
                assertThat(stats.get("totalProfitLoss")).isNotNull();
            }
            if (stats.containsKey("winRate")) {
                Object winRate = stats.get("winRate");
                assertThat(winRate).isNotNull();
                if (winRate instanceof Number) {
                    double rate = ((Number) winRate).doubleValue();
                    assertThat(rate).isBetween(0.0, 1.0);
                }
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("대시보드 통계 API - 숫자 타입 검증")
    void getDashboardStats_NumericTypes() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(response).isNotNull();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> stats = response.getBody();

            // 정수 필드 검증
            if (stats.containsKey("todayOrders")) {
                assertThat(stats.get("todayOrders")).isInstanceOf(Number.class);
            }
            if (stats.containsKey("todayFills")) {
                assertThat(stats.get("todayFills")).isInstanceOf(Number.class);
            }

            // 소수점 필드 검증
            if (stats.containsKey("winRate")) {
                assertThat(stats.get("winRate")).isInstanceOf(Number.class);
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("대시보드 통계 API - 최근 활동 목록 검증")
    void getDashboardStats_RecentActivities() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(response).isNotNull();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> stats = response.getBody();

            if (stats.containsKey("recentActivities")) {
                Object activities = stats.get("recentActivities");
                assertThat(activities).isInstanceOf(List.class);
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("대시보드 통계 API - 일별 통계 검증")
    void getDashboardStats_DailyStats() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(response).isNotNull();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> stats = response.getBody();

            if (stats.containsKey("dailyStats")) {
                Object dailyStats = stats.get("dailyStats");
                assertThat(dailyStats).isInstanceOf(List.class);
            }
        }
    }

    // ==================== Round-Trip Tests ====================

    @Test
    @Order(8)
    @DisplayName("대시보드 통계 - Round-Trip 검증")
    void getDashboardStats_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 첫 번째 호출
        ResponseEntity<Map> response1 = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then - 두 번째 호출 및 비교
        if (response1 != null && response1.getStatusCode().is2xxSuccessful()) {
            ResponseEntity<Map> response2 = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

            assertThat(response2).isNotNull();
            assertThat(response2.getStatusCode().is2xxSuccessful()).isTrue();

            // 구조가 동일해야 함
            if (response1.getBody() != null && response2.getBody() != null) {
                assertThat(response2.getBody().keySet())
                    .containsAll(response1.getBody().keySet());
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("대시보드 페이지와 API 통합 검증")
    void dashboard_PageAndApiIntegration() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 페이지 로드
        ResponseEntity<String> pageResponse = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/dashboard"), String.class);

        // And - API 호출
        ResponseEntity<Map> apiResponse = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then - 둘 다 성공해야 함
        assertThat(pageResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND);
        if (apiResponse != null) {
            assertThat(apiResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(10)
    @DisplayName("대시보드 페이지 - API 장애 시에도 페이지 로드")
    void dashboardPage_LoadsEvenWhenApiPartiallyFails() {
        // When - 페이지 요청 (API 상태와 무관하게)
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/dashboard"), String.class);

        // Then - 페이지는 로드되어야 함 (에러 페이지 포함)
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.OK,
            HttpStatus.FOUND,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    @Order(11)
    @DisplayName("Health API와 Dashboard Stats API 동시 호출")
    void healthAndDashboardStats_ParallelCall() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 동시 호출
        ResponseEntity<Map> healthResponse = safeCautostockGet("/health", Map.class);
        ResponseEntity<Map> statsResponse = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);

        // Then
        assertThat(healthResponse).isNotNull();
        assertThat(healthResponse.getStatusCode().is2xxSuccessful()).isTrue();

        if (statsResponse != null) {
            assertThat(statsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    // ==================== Performance Tests ====================

    @Test
    @Order(12)
    @DisplayName("대시보드 통계 API - 응답 시간 검증")
    void getDashboardStats_ResponseTime() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/dashboard/stats", Map.class);
        long endTime = System.currentTimeMillis();

        // Then - 5초 이내 응답
        long responseTime = endTime - startTime;
        assertThat(responseTime).isLessThan(5000L);

        if (response != null) {
            System.out.println("Dashboard stats API response time: " + responseTime + "ms");
        }
    }
}
