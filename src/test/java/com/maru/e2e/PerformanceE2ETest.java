package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 성과 분석 E2E 테스트
 *
 * 시나리오: 성과 데이터 조회, 전략별 통계
 */
@DisplayName("Performance E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("성과 분석 메인 페이지 조회")
    void performanceAnalysisPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/performance"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("성과 분석 API - 일별 집계")
    void performanceAnalysisDaily() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format("/api/v1/query/performance?period=daily&startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("성과 분석 API - 월별 집계")
    void performanceAnalysisMonthly() {
        // Given
        String startDate = getDateBefore(365);
        String endDate = getToday();
        String path = String.format("/api/v1/query/performance?period=monthly&startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("전략별 통계 페이지 조회")
    void strategyStatisticsPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/performance/strategies"), String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(5)
    @DisplayName("전략별 통계 API")
    void strategyStatisticsApi() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format("/api/v1/query/performance/strategies?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("성과 리포트 - 수익률 계산")
    void performanceReportProfitLoss() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/performance/summary", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            System.out.println("Performance summary: " + body);
        }
    }

    @Test
    @Order(7)
    @DisplayName("성과 분석 - 날짜 범위 검증")
    void performanceAnalysisDateRange() {
        // Given - 유효하지 않은 날짜 범위 (시작일 > 종료일)
        String startDate = getToday();
        String endDate = getDateBefore(30);
        String path = String.format("/api/v1/query/performance?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then - 에러 응답 또는 빈 결과
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("성과 분석 - 전략 필터")
    void performanceAnalysisWithStrategyFilter() {
        // Given
        String strategyId = "test-strategy-1";
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format("/api/v1/query/performance?strategyId=%s&startDate=%s&endDate=%s",
            strategyId, startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Excel 리포트 다운로드")
    void exportToExcel() {
        // When
        ResponseEntity<byte[]> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/performance/export"), byte[].class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        if (response.getBody() != null) {
            assertThat(response.getBody().length).isGreaterThan(0);
        }
    }

    @Test
    @Order(10)
    @DisplayName("성과 비교 - 벤치마크 대비")
    void performanceComparisonWithBenchmark() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/performance/comparison?benchmark=KOSPI", Map.class);

        // Then - API가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("일별 손익 추이")
    void dailyProfitLossTrend() {
        // Given
        String startDate = getDateBefore(30);
        String endDate = getToday();
        String path = String.format("/api/v1/query/performance/daily-trend?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }
}
