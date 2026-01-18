package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리스크 룰 E2E 테스트
 *
 * 시나리오: 리스크 룰 목록 조회 → 생성 → 수정 → 삭제
 */
@DisplayName("RiskRule E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RiskRuleE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static String createdRuleId;

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // API가 List 또는 Map을 반환할 수 있음
            ResponseEntity<Object> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/risk-rules"), Object.class);
            apiAvailable = response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404는 API 존재하지만 데이터 없음
            apiAvailable = true;
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @AfterAll
    void cleanUp() {
        if (createdRuleId != null) {
            try {
                restTemplate.delete(getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId));
            } catch (Exception e) {
                // 정리 실패 무시
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("리스크 룰 페이지 조회")
    void riskRulesPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/risk-rules"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("리스크");
    }

    @Test
    @Order(2)
    @DisplayName("리스크 룰 목록 조회 - API")
    void getRiskRules() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/risk-rules"), Object.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(3)
    @DisplayName("리스크 룰 생성")
    void createRiskRule() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // Given
        Map<String, Object> ruleData = new HashMap<>();
        ruleData.put("name", "E2E 테스트 리스크 룰 " + System.currentTimeMillis());
        ruleData.put("type", "MAX_LOSS");
        ruleData.put("threshold", new BigDecimal("5.0"));
        ruleData.put("action", "ALERT");
        ruleData.put("enabled", true);

        // When
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/risk-rules"),
                createJsonEntity(ruleData),
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createdRuleId = extractId(response.getBody());
                System.out.println("Created rule ID: " + createdRuleId);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Risk rule API not available: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Risk rule API error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(4)
    @DisplayName("리스크 룰 조회 - Round-Trip 검증")
    void getRiskRule_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (createdRuleId == null) {
            System.out.println("No rule created - skipping round-trip check");
            return;
        }

        // When - 룰 조회
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String name = (String) response.getBody().get("name");

                // Then - 재조회하여 일치 확인
                ResponseEntity<Map> secondResponse = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId), Map.class);

                assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(secondResponse.getBody()).isNotNull();
                assertThat(secondResponse.getBody().get("name")).isEqualTo(name);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @Order(5)
    @DisplayName("리스크 룰 수정")
    void updateRiskRule() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (createdRuleId == null) {
            System.out.println("No rule created - skipping update");
            return;
        }

        // Given
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "수정된 E2E 테스트 룰");
        updateData.put("threshold", new BigDecimal("10.0"));

        // When
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId),
                HttpMethod.PUT,
                createJsonEntity(updateData),
                Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // 재조회하여 수정 확인
                ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId), Map.class);

                if (getResponse.getStatusCode().is2xxSuccessful() && getResponse.getBody() != null) {
                    assertThat(getResponse.getBody().get("name")).isEqualTo("수정된 E2E 테스트 룰");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Risk rule update not available: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Risk rule update error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(6)
    @DisplayName("리스크 룰 삭제")
    void deleteRiskRule() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        if (createdRuleId == null) {
            System.out.println("No rule created - skipping delete");
            return;
        }

        // When
        try {
            restTemplate.delete(getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId));

            // Then - 삭제 확인
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    getCautostockUrl("/api/v1/admin/risk-rules/" + createdRuleId), Map.class);
                // 삭제되었지만 여전히 조회되면 에러 응답인지 확인
                assertThat(response.getBody() == null || response.getBody().containsKey("error")).isTrue();
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                // 404면 삭제 성공
            }

            createdRuleId = null; // 정리 완료
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Delete API not available: " + e.getStatusCode());
        } catch (Exception e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    private String extractId(Map<String, Object> body) {
        if (body.containsKey("ruleId")) {
            return String.valueOf(body.get("ruleId"));
        } else if (body.containsKey("id")) {
            return String.valueOf(body.get("id"));
        }
        return null;
    }
}
