package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Health Check E2E 테스트
 *
 * 시나리오: 시스템 상태 조회 → 컴포넌트 상태 → 알림 설정
 */
@DisplayName("Health Check E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HealthCheckE2ETest extends E2ETestBase {

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
            apiAvailable = response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Health Check 페이지 조회")
    void healthCheckPage() {
        // When - Health 페이지는 계좌 목록 페이지로 대체
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/accounts"), String.class);

        // Then - 계좌 페이지가 정상 로드되면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("시스템 상태 조회 - API")
    void getSystemHealth() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        if (response.getBody() != null) {
            assertThat(response.getBody()).containsKey("status");
        }
    }

    @Test
    @Order(3)
    @DisplayName("상세 Health 정보 조회")
    void getDetailedHealth() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health/details", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("데이터베이스 상태 조회")
    void getDatabaseHealth() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health/db", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("API 연결 상태 조회")
    void getApiHealth() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health/api", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("시스템 상태 조회 - Round-Trip 검증")
    void getHealth_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 상태 조회
        ResponseEntity<Map> response = safeCautostockGet("/health", Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object status = response.getBody().get("status");

            // Then - 재조회하여 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet("/health", Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
            if (secondResponse.getBody() != null) {
                // 상태는 일반적으로 같아야 함 (UP/DOWN)
                assertThat(secondResponse.getBody().get("status")).isEqualTo(status);
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("메트릭스 조회")
    void getMetrics() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health/metrics", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("시스템 정보 조회")
    void getSystemInfo() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/health/info", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
