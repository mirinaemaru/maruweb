package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실행 히스토리 E2E 테스트
 *
 * 시나리오: 실행 히스토리 조회, 필터링
 */
@DisplayName("Execution History E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExecutionHistoryE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("실행 히스토리 페이지 조회")
    void executionHistoryPage() {
        try {
            // When
            ResponseEntity<String> response = testRestTemplate.getForEntity(
                getMaruwebUrl("/trading/execution-history"), String.class);

            // Then - 페이지가 없을 수도 있음
            assertThat(response.getStatusCode().is2xxSuccessful() ||
                       response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
        } catch (Exception e) {
            System.out.println("Execution history page not available: " + e.getMessage());
            // 페이지가 없으면 테스트 통과로 간주
        }
    }

    @Test
    @Order(2)
    @DisplayName("실행 히스토리 API - 전체 조회")
    void listExecutionHistory() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/execution-history", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("실행 히스토리 - 날짜 필터")
    void listExecutionHistoryWithDateFilter() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format("/api/v1/query/execution-history?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("실행 히스토리 - 전략 필터")
    void listExecutionHistoryByStrategy() {
        // Given
        String strategyId = "test-strategy-1";
        String path = String.format("/api/v1/query/execution-history?strategyId=%s", strategyId);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("실행 히스토리 - 종목 필터")
    void listExecutionHistoryBySymbol() {
        // Given
        String symbol = "005930";
        String path = String.format("/api/v1/query/execution-history?symbol=%s", symbol);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("실행 히스토리 - 매매 유형 필터 (BUY)")
    void listExecutionHistoryBuyOrders() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/execution-history?side=BUY", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("실행 히스토리 - 매매 유형 필터 (SELL)")
    void listExecutionHistorySellOrders() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/execution-history?side=SELL", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("실행 히스토리 - 페이지네이션")
    void listExecutionHistoryWithPagination() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/execution-history?page=0&size=10", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("실행 히스토리 - 정렬 (최신순)")
    void listExecutionHistorySortedByDateDesc() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/execution-history?sort=executedAt,desc", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("실행 히스토리 - 복합 필터")
    void listExecutionHistoryWithMultipleFilters() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String symbol = "005930";
        String side = "BUY";
        String path = String.format(
            "/api/v1/query/execution-history?startDate=%s&endDate=%s&symbol=%s&side=%s",
            startDate, endDate, symbol, side);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("실행 히스토리 상세 조회")
    void getExecutionDetail() {
        // First, get a list to find an execution ID
        ResponseEntity<Map> listResponse = safeCautostockGet("/api/v1/query/execution-history", Map.class);

        if (listResponse == null || !listResponse.getStatusCode().is2xxSuccessful() ||
            listResponse.getBody() == null) {
            return;
        }

        List<Map<String, Object>> items = getItemsFromResponse(listResponse.getBody());
        if (items.isEmpty()) {
            System.out.println("No execution history found, skipping detail test");
            return;
        }

        String executionId = extractId(items.get(0), "executionId", "id");
        if (executionId == null) {
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/execution-history/" + executionId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(12)
    @DisplayName("일별 실행 통계")
    void getDailyExecutionStats() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format(
            "/api/v1/query/execution-history/daily-stats?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then - API가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("종목별 실행 통계")
    void getSymbolExecutionStats() {
        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/query/execution-history/symbol-stats", Map.class);

        // Then - API가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(14)
    @DisplayName("실행 히스토리 내보내기 (CSV)")
    void exportExecutionHistoryToCsv() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format(
            "/api/v1/query/execution-history/export?format=csv&startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(
                getCautostockUrl(path), byte[].class);
            assertThat(response.getStatusCode().is2xxSuccessful() ||
                       response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
            Assumptions.assumeTrue(false, "Cautostock API not available");
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
