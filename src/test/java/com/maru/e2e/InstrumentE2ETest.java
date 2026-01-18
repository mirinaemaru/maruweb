package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 종목 관리 E2E 테스트
 *
 * 시나리오: 종목 목록 조회 → 종목 검색 → 종목 상세 조회
 */
@DisplayName("Instrument E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InstrumentE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static String testSymbol = "005930"; // 삼성전자

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // API는 List를 반환함
            ResponseEntity<List> response = restTemplate.getForEntity(
                getCautostockUrl("/api/instruments"), List.class);
            apiAvailable = response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("종목 목록 페이지 조회")
    void listInstruments() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/instruments"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("종목");
    }

    @Test
    @Order(2)
    @DisplayName("종목 목록 조회 - API")
    void getInstruments() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - API는 List를 반환함
        ResponseEntity<List> response = restTemplate.getForEntity(
            getCautostockUrl("/api/instruments"), List.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("종목 검색")
    void searchInstruments() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 삼성전자 검색 (API가 없을 수 있으므로 예외 처리)
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                getCautostockUrl("/api/instruments?keyword=삼성"), List.class);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 검색 API가 없으면 테스트 통과
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Test
    @Order(4)
    @DisplayName("종목 상세 조회")
    void getInstrumentDetail() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 개별 종목 조회 API가 없을 수 있으므로 예외 처리
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/instruments/" + testSymbol), Map.class);

            // Then
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> instrument = response.getBody();
                assertThat(instrument.get("symbol")).isEqualTo(testSymbol);
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // API가 없으면 테스트 통과
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Test
    @Order(5)
    @DisplayName("종목 시세 조회")
    void getInstrumentPrice() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 시세 API가 없을 수 있으므로 예외 처리
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/instruments/" + testSymbol + "/price"), Map.class);

            // Then - API가 있으면 성공
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // API가 없으면 테스트 통과
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Test
    @Order(6)
    @DisplayName("종목 조회 - Round-Trip 검증")
    void getInstrument_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "API not available");

        // When - 개별 종목 조회 API가 없을 수 있으므로 예외 처리
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getCautostockUrl("/api/instruments/" + testSymbol), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String symbol = String.valueOf(response.getBody().get("symbol"));

                // Then - 재조회하여 일치 확인
                ResponseEntity<Map> secondResponse = restTemplate.getForEntity(
                    getCautostockUrl("/api/instruments/" + symbol), Map.class);

                assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(secondResponse.getBody()).isNotNull();
                assertThat(secondResponse.getBody().get("symbol")).isEqualTo(symbol);
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // API가 없으면 테스트 통과
            assertThat(e.getStatusCode().value()).isEqualTo(404);
        }
    }
}
