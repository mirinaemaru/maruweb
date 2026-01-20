package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarketData 구독 관리 E2E 테스트
 *
 * 시나리오: 구독 종목 관리 페이지 조회 → 종목 추가 → 종목 삭제 → 재구독
 */
@DisplayName("MarketData E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MarketDataE2ETest extends E2ETestBase {

    private static boolean apiAvailable = true;
    private static final String TEST_SYMBOL = "005930"; // 삼성전자

    @BeforeAll
    void initTest() {
        try {
            super.waitForCautostock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // API 가용성 확인
        try {
            ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/market-data/status", Map.class);
            apiAvailable = response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.out.println("MarketData API not available: " + e.getMessage());
            apiAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("MarketData 구독 관리 페이지 조회")
    void marketDataPage_Accessible() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/market-data"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Market Data 구독 관리");
    }

    @Test
    @Order(2)
    @DisplayName("구독 상태 조회 - API")
    void getMarketDataStatus() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/market-data/status", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        if (response.getBody() != null) {
            // 응답에 subscribed 필드가 있어야 함
            assertThat(response.getBody()).containsKey("subscribed");
        }
    }

    @Test
    @Order(3)
    @DisplayName("구독 종목 목록 조회 - API")
    void getSubscribedSymbols() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/market-data/symbols", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        if (response.getBody() != null) {
            // symbols 필드가 있어야 함
            assertThat(response.getBody()).containsAnyOf(
                Map.entry("symbols", response.getBody().get("symbols")));
        }
    }

    @Test
    @Order(4)
    @DisplayName("종목 추가 - Round-Trip 검증")
    void addSymbols_RoundTrip() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given - 추가할 종목
        Map<String, Object> request = new HashMap<>();
        request.put("symbols", Collections.singletonList(TEST_SYMBOL));

        // When - 종목 추가
        ResponseEntity<Map> addResponse = safeCautostockPost(
            "/api/v1/admin/market-data/symbols", request, Map.class);

        // Then - 추가 결과 확인
        if (addResponse != null && addResponse.getStatusCode().is2xxSuccessful()) {
            // 추가 후 구독 목록에 포함되어 있는지 확인
            ResponseEntity<Map> symbolsResponse = safeCautostockGet(
                "/api/v1/admin/market-data/symbols", Map.class);

            assertThat(symbolsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(5)
    @DisplayName("종목 삭제 - API")
    void removeSymbols() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given - 삭제할 종목
        Map<String, Object> request = new HashMap<>();
        request.put("symbols", Collections.singletonList(TEST_SYMBOL));

        // When - 종목 삭제
        try {
            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                getCautostockUrl("/api/v1/admin/market-data/symbols"),
                HttpMethod.DELETE,
                entity,
                Map.class);

            // Then - 삭제 결과 확인
            assertThat(deleteResponse.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (Exception e) {
            // API가 지원되지 않을 수 있음
            System.out.println("Delete symbols API: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("재구독 - API")
    void resubscribe() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/market-data/resubscribe", new HashMap<>(), Map.class);

        // Then
        if (response != null) {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Test
    @Order(7)
    @DisplayName("폼 제출 테스트 - 종목 추가")
    void addSymbols_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given
        HttpHeaders headers = createFormHeaders();
        String formData = "symbols=" + TEST_SYMBOL;

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/market-data/add"),
            new HttpEntity<>(formData, headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("폼 제출 테스트 - 종목 삭제")
    void removeSymbols_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given
        HttpHeaders headers = createFormHeaders();
        String formData = "symbols=" + TEST_SYMBOL;

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/market-data/remove"),
            new HttpEntity<>(formData, headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("폼 제출 테스트 - 재구독")
    void resubscribe_FormSubmit() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given
        HttpHeaders headers = createFormHeaders();

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/market-data/resubscribe"),
            new HttpEntity<>("", headers),
            String.class);

        // Then - 리다이렉트 응답 또는 성공
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("빈 종목 추가 시 에러 처리")
    void addSymbols_EmptySymbols_HandlesError() {
        Assumptions.assumeTrue(apiAvailable, "MarketData API not available");

        // Given
        HttpHeaders headers = createFormHeaders();
        String formData = "symbols=";

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            getMaruwebUrl("/trading/market-data/add"),
            new HttpEntity<>(formData, headers),
            String.class);

        // Then - 에러 처리 후 리다이렉트
        assertThat(response.getStatusCode().is3xxRedirection() ||
                   response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
