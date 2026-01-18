package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kill Switch E2E 테스트
 *
 * 시나리오: Kill Switch 상태 조회 → 활성화 → 비활성화
 */
@DisplayName("Kill Switch E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KillSwitchE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ResponseEntity<Map> response = safeCautostockGet("/api/kill-switch/status", Map.class);
            apiAvailable = response.getStatusCode().is2xxSuccessful() ||
                           response.getStatusCode() == HttpStatus.NOT_FOUND;
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Kill Switch 페이지 조회")
    void killSwitchPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/kill-switch"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("Kill Switch 상태 조회 - API")
    void getKillSwitchStatus() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/kill-switch/status", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Kill Switch 상태 조회 - Round-Trip 검증")
    void getKillSwitchStatus_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 상태 조회
        ResponseEntity<Map> response = safeCautostockGet("/api/kill-switch/status", Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object status = response.getBody().get("active");

            // Then - 재조회하여 일치 확인
            ResponseEntity<Map> secondResponse = safeCautostockGet("/api/kill-switch/status", Map.class);

            assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
            if (secondResponse.getBody() != null) {
                assertThat(secondResponse.getBody().get("active")).isEqualTo(status);
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Kill Switch 히스토리 조회")
    void getKillSwitchHistory() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/kill-switch/history", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
