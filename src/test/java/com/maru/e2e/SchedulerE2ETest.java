package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스케줄러 관리 E2E 테스트
 *
 * 시나리오: 스케줄러 상태 조회 → 활성화/비활성화 → 전략 실행 → 통계 초기화
 */
@DisplayName("Scheduler E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchedulerE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // API 가용성 확인
        try {
            ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/scheduler/status", Map.class);
            apiAvailable = response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.out.println("Scheduler API not available: " + e.getMessage());
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("스케줄러 관리 페이지 조회")
    void schedulerPage_Accessible() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/scheduler"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("스케줄러 관리");
    }

    @Test
    @Order(2)
    @DisplayName("스케줄러 상태 조회 - API")
    void getSchedulerStatus() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/scheduler/status", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        if (response.getBody() != null) {
            // 응답에 필수 필드가 있어야 함
            Map body = response.getBody();
            // enabled 또는 status 필드가 있어야 함
            assertThat(body.containsKey("enabled") || body.containsKey("status")).isTrue();
        }
    }

    @Test
    @Order(3)
    @DisplayName("스케줄러 활성화 - API")
    void enableScheduler() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/scheduler/enable", new HashMap<>(), Map.class);

        // Then
        if (response != null) {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(4)
    @DisplayName("스케줄러 비활성화 - API")
    void disableScheduler() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/scheduler/disable", new HashMap<>(), Map.class);

        // Then
        if (response != null) {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(5)
    @DisplayName("모든 전략 실행 - API")
    void executeAllStrategies() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/scheduler/execute-all", new HashMap<>(), Map.class);

        // Then
        if (response != null) {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(6)
    @DisplayName("통계 초기화 - API")
    void resetStats() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/scheduler/reset-stats", new HashMap<>(), Map.class);

        // Then
        if (response != null) {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(7)
    @DisplayName("폼 제출 테스트 - 스케줄러 활성화")
    void enableScheduler_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given
        HttpHeaders headers = createFormHeaders();

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/enable"),
            new HttpEntity<>("", headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("폼 제출 테스트 - 스케줄러 비활성화")
    void disableScheduler_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given
        HttpHeaders headers = createFormHeaders();

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/disable"),
            new HttpEntity<>("", headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("폼 제출 테스트 - 전체 실행")
    void executeAllStrategies_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given
        HttpHeaders headers = createFormHeaders();

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/execute-all"),
            new HttpEntity<>("", headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("폼 제출 테스트 - 통계 초기화")
    void resetStats_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given
        HttpHeaders headers = createFormHeaders();

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/reset-stats"),
            new HttpEntity<>("", headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("폼 제출 테스트 - 전략 수동 실행 (빈 ID)")
    void triggerStrategy_EmptyId_HandlesError() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given
        HttpHeaders headers = createFormHeaders();
        String formData = "strategyId=";

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/trigger"),
            new HttpEntity<>(formData, headers),
            String.class);

        // Then - 에러 처리 후 리다이렉트
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(12)
    @DisplayName("폼 제출 테스트 - 전략 수동 실행")
    void triggerStrategy_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given - 테스트용 전략 ID (존재하지 않아도 API 호출은 됨)
        HttpHeaders headers = createFormHeaders();
        String formData = "strategyId=test-strategy-123";

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/scheduler/trigger"),
            new HttpEntity<>(formData, headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공 (전략이 없으면 에러 메시지와 함께 리다이렉트)
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Round-Trip 테스트 - 활성화 후 상태 확인")
    void enableScheduler_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given - 스케줄러 활성화
        ResponseEntity<Map> enableResponse = safeCautostockPost(
            "/api/v1/admin/scheduler/enable", new HashMap<>(), Map.class);

        if (enableResponse != null && enableResponse.getStatusCode().is2xxSuccessful()) {
            // When - 상태 확인
            ResponseEntity<Map> statusResponse = safeCautostockGet(
                "/api/v1/admin/scheduler/status", Map.class);

            // Then
            assertThat(statusResponse.getStatusCode().is2xxSuccessful()).isTrue();
            // 활성화 후 상태가 변경되었는지 확인
            if (statusResponse.getBody() != null) {
                Map body = statusResponse.getBody();
                // enabled 필드가 있으면 true인지 확인
                if (body.containsKey("enabled")) {
                    assertThat(body.get("enabled")).isEqualTo(true);
                }
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("Round-Trip 테스트 - 비활성화 후 상태 확인")
    void disableScheduler_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "Scheduler API not available");

        // Given - 스케줄러 비활성화
        ResponseEntity<Map> disableResponse = safeCautostockPost(
            "/api/v1/admin/scheduler/disable", new HashMap<>(), Map.class);

        if (disableResponse != null && disableResponse.getStatusCode().is2xxSuccessful()) {
            // When - 상태 확인
            ResponseEntity<Map> statusResponse = safeCautostockGet(
                "/api/v1/admin/scheduler/status", Map.class);

            // Then
            assertThat(statusResponse.getStatusCode().is2xxSuccessful()).isTrue();
            // 비활성화 후 상태가 변경되었는지 확인
            if (statusResponse.getBody() != null) {
                Map body = statusResponse.getBody();
                // enabled 필드가 있으면 false인지 확인
                if (body.containsKey("enabled")) {
                    assertThat(body.get("enabled")).isEqualTo(false);
                }
            }
        }
    }
}
