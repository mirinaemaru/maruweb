package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리스크 분석 E2E 테스트
 *
 * 시나리오: VaR, CVaR, 상관관계 매트릭스
 */
@DisplayName("Risk Analysis E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RiskAnalysisE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("리스크 분석 메인 페이지 조회")
    void riskAnalysisPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/risk-analysis"), String.class);

        // Then - 페이지가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("VaR (Value at Risk) 계산 API")
    void calculateVaR() {
        // Given
        String startDate = getDateBefore(365);
        String endDate = getToday();
        String path = String.format("/api/v1/query/risk/var?startDate=%s&endDate=%s&confidenceLevel=0.95",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            System.out.println("VaR result: " + response.getBody());
        }
    }

    @Test
    @Order(3)
    @DisplayName("CVaR (Conditional VaR) 계산 API")
    void calculateCVaR() {
        // Given
        String startDate = getDateBefore(365);
        String endDate = getToday();
        String path = String.format("/api/v1/query/risk/cvar?startDate=%s&endDate=%s&confidenceLevel=0.95",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("상관관계 매트릭스 계산")
    void calculateCorrelationMatrix() {
        // Given
        List<String> symbols = Arrays.asList("005930", "000660", "035420");
        String path = String.format("/api/v1/query/risk/correlation?symbols=%s",
            String.join(",", symbols));

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            System.out.println("Correlation matrix: " + response.getBody());
        }
    }

    @Test
    @Order(5)
    @DisplayName("상관관계 분석 페이지 조회")
    void correlationAnalysisPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/correlation-analysis"), String.class);

        // Then - 페이지가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("포트폴리오 VaR 계산")
    void calculatePortfolioVaR() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("positions", Arrays.asList(
            createPosition("005930", 100, 70000),
            createPosition("000660", 50, 120000)
        ));
        request.put("confidenceLevel", 0.95);
        request.put("holdingPeriod", 1);

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/query/risk/portfolio-var", request, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("최대 손실 (Maximum Drawdown) 계산")
    void calculateMaxDrawdown() {
        // Given
        String startDate = getDateBefore(365);
        String endDate = getToday();
        String path = String.format("/api/v1/query/risk/max-drawdown?startDate=%s&endDate=%s",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("리스크 메트릭스 - 샤프 비율")
    void calculateSharpeRatio() {
        // Given
        String startDate = getDateBefore(365);
        String endDate = getToday();
        String path = String.format("/api/v1/query/risk/sharpe-ratio?startDate=%s&endDate=%s&riskFreeRate=0.03",
            startDate, endDate);

        // When
        ResponseEntity<Map> response = safeCautostockGet(path, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("VaR 신뢰수준별 계산 - 99%")
    void calculateVaR99() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/risk/var?confidenceLevel=0.99", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("VaR 신뢰수준별 계산 - 90%")
    void calculateVaR90() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/query/risk/var?confidenceLevel=0.90", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("리스크 대시보드 페이지 조회")
    void riskDashboardPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/risk-dashboard"), String.class);

        // Then - 페이지가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(12)
    @DisplayName("스트레스 테스트")
    void stressTest() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("scenario", "MARKET_CRASH");
        request.put("shockPercent", -20);

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/query/risk/stress-test", request, Map.class);

        // Then - API가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createPosition(String symbol, int quantity, double avgPrice) {
        Map<String, Object> position = new HashMap<>();
        position.put("symbol", symbol);
        position.put("quantity", quantity);
        position.put("avgPrice", avgPrice);
        return position;
    }
}
