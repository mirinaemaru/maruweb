package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BacktestController 통합테스트")
class BacktestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ========== 백테스트 결과 목록 ==========

    @Test
    @DisplayName("백테스트 결과 목록 조회 - 성공")
    void listBacktests_Success() throws Exception {
        // Given
        List<Map<String, Object>> backtests = new ArrayList<>();
        backtests.add(createBacktestMock("bt-1", "strategy-1", "2024-01-01", "2024-01-31"));
        Map<String, Object> result = new HashMap<>();
        result.put("backtests", backtests);

        when(tradingApiService.getBacktestResults(isNull(), isNull(), isNull())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/backtests"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests"))
                .andExpect(model().attributeExists("backtests"));
    }

    @Test
    @DisplayName("백테스트 결과 목록 조회 - 필터링")
    void listBacktests_WithFilters() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("backtests", Collections.emptyList());

        when(tradingApiService.getBacktestResults(eq("strategy-1"), eq("2024-01-01"), eq("2024-01-31")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/backtests")
                        .param("strategyId", "strategy-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests"))
                .andExpect(model().attribute("strategyId", "strategy-1"));
    }

    @Test
    @DisplayName("백테스트 결과 목록 조회 - API 오류")
    void listBacktests_ApiError() throws Exception {
        // Given
        when(tradingApiService.getBacktestResults(any(), any(), any()))
                .thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/backtests"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 백테스트 상세 ==========

    @Test
    @DisplayName("백테스트 상세 조회 - 성공")
    void backtestDetail_Success() throws Exception {
        // Given
        Map<String, Object> backtest = createBacktestDetailMock("bt-1", "strategy-1");

        when(tradingApiService.getBacktestDetail(1L)).thenReturn(backtest);

        // When & Then
        mockMvc.perform(get("/trading/backtests/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-detail"))
                .andExpect(model().attributeExists("backtest"))
                .andExpect(model().attributeExists("trades"))
                .andExpect(model().attributeExists("metrics"));
    }

    @Test
    @DisplayName("백테스트 상세 조회 - API 오류")
    void backtestDetail_ApiError() throws Exception {
        // Given - API 호출 시 예외 발생
        when(tradingApiService.getBacktestDetail(anyLong()))
                .thenThrow(new RuntimeException("백테스트를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/trading/backtests/999"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-detail"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Walk-Forward Analysis ==========

    @Test
    @DisplayName("Walk-Forward Analysis 페이지 조회")
    void walkForwardPage_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trading/backtests/advanced/walk-forward"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/walk-forward-analysis"));
    }

    @Test
    @DisplayName("Walk-Forward Analysis 실행 - 성공")
    void runWalkForward_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("windows", Collections.emptyList());
        result.put("summary", Map.of("totalReturn", 0.15));

        when(tradingApiService.runWalkForwardAnalysis(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/advanced/walk-forward")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("strategyId", "strategy-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30")
                        .param("windowDays", "30")
                        .param("stepDays", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/walk-forward-result"))
                .andExpect(model().attributeExists("result"));
    }

    @Test
    @DisplayName("Walk-Forward Analysis 실행 - 오류 응답")
    void runWalkForward_ErrorResponse() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("error", "데이터 부족");

        when(tradingApiService.runWalkForwardAnalysis(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/advanced/walk-forward")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("strategyId", "strategy-1")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/walk-forward-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== Portfolio Backtest ==========

    @Test
    @DisplayName("Portfolio Backtest 페이지 조회")
    void portfolioBacktestPage_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trading/backtests/advanced/portfolio"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/portfolio-backtest"));
    }

    @Test
    @DisplayName("Portfolio Backtest 실행 - 성공")
    void runPortfolioBacktest_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("portfolioReturn", 0.12);
        result.put("sharpeRatio", 1.5);

        when(tradingApiService.runPortfolioBacktest(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/advanced/portfolio")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbols", "005930,000660,035720")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30")
                        .param("allocationType", "EQUAL"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/portfolio-backtest-result"))
                .andExpect(model().attributeExists("result"));
    }

    @Test
    @DisplayName("Portfolio Backtest 실행 - 오류 응답")
    void runPortfolioBacktest_ErrorResponse() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("error", "종목 데이터 부족");

        when(tradingApiService.runPortfolioBacktest(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/advanced/portfolio")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbols", "005930")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/portfolio-backtest"))
                .andExpect(model().attributeExists("error"));
    }

    // ========== 백테스트 거래 상세 ==========

    @Test
    @DisplayName("백테스트 거래 상세 조회 - 성공")
    void backtestTrades_Success() throws Exception {
        // Given
        Map<String, Object> backtest = createBacktestDetailMock("bt-1", "strategy-1");
        Map<String, Object> tradesResult = new HashMap<>();
        tradesResult.put("trades", createTradesList());

        when(tradingApiService.getBacktestDetail(1L)).thenReturn(backtest);
        when(tradingApiService.getBacktestTrades(1L)).thenReturn(tradesResult);

        // When & Then
        mockMvc.perform(get("/trading/backtests/1/trades"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-trades"))
                .andExpect(model().attributeExists("backtest"))
                .andExpect(model().attributeExists("trades"));
    }

    // ========== Admin 백테스트 ==========

    @Test
    @DisplayName("Admin 백테스트 목록 조회 - 성공")
    void adminList_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("backtests", Collections.emptyList());

        when(tradingApiService.listBacktests()).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/backtests/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests-admin"))
                .andExpect(model().attributeExists("backtests"));
    }

    @Test
    @DisplayName("Admin 백테스트 목록 조회 - API 타임아웃")
    void adminList_ApiTimeout() throws Exception {
        // Given
        when(tradingApiService.listBacktests())
                .thenThrow(new RuntimeException("Connection timeout"));

        // When & Then
        mockMvc.perform(get("/trading/backtests/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests-admin"))
                .andExpect(model().attributeExists("warning"));
    }

    @Test
    @DisplayName("Admin 백테스트 실행 페이지 조회")
    void adminRunPage_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trading/backtests/admin/run"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-run"));
    }

    @Test
    @DisplayName("Admin 백테스트 실행 - 성공")
    void adminRunBacktest_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("backtestId", "bt-new");

        when(tradingApiService.runBacktest(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/admin/run")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("strategyId", "strategy-1")
                        .param("symbols", "005930,000660")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30")
                        .param("timeframe", "1d")
                        .param("initialCapital", "10000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/backtests/admin"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Admin 백테스트 실행 - 오류")
    void adminRunBacktest_Error() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("error", "전략을 찾을 수 없습니다");

        when(tradingApiService.runBacktest(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/admin/run")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("strategyId", "invalid-strategy")
                        .param("symbols", "005930")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/backtests/admin/run"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Admin 백테스트 상세 조회 - 성공")
    void adminDetail_Success() throws Exception {
        // Given
        Map<String, Object> backtest = createBacktestDetailMock("bt-1", "strategy-1");

        when(tradingApiService.getBacktest("bt-1")).thenReturn(backtest);

        // When & Then
        mockMvc.perform(get("/trading/backtests/admin/bt-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-admin-detail"))
                .andExpect(model().attributeExists("backtest"));
    }

    @Test
    @DisplayName("Admin 백테스트 삭제 - 성공")
    void adminDelete_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        when(tradingApiService.deleteBacktest("bt-1")).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/admin/bt-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/backtests/admin"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Admin 백테스트 삭제 - 오류")
    void adminDelete_Error() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("error", "삭제 권한이 없습니다");

        when(tradingApiService.deleteBacktest("bt-1")).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/backtests/admin/bt-1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/backtests/admin"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createBacktestMock(String id, String strategyId, String startDate, String endDate) {
        Map<String, Object> backtest = new HashMap<>();
        backtest.put("id", id);
        backtest.put("strategyId", strategyId);
        backtest.put("strategyName", "테스트 전략");  // 템플릿에서 사용
        backtest.put("startDate", startDate);
        backtest.put("endDate", endDate);
        backtest.put("status", "COMPLETED");
        backtest.put("totalReturn", 0.15);
        backtest.put("maxDrawdown", -0.05);
        backtest.put("sharpeRatio", 1.8);
        backtest.put("winRate", 0.65);
        backtest.put("totalTrades", 50);
        backtest.put("totalProfitLoss", new BigDecimal("1500000"));  // 템플릿에서 사용
        backtest.put("createdAt", "2024-01-01T10:00:00");
        return backtest;
    }

    private Map<String, Object> createBacktestDetailMock(String id, String strategyId) {
        Map<String, Object> backtest = createBacktestMock(id, strategyId, "2024-01-01", "2024-06-30");
        backtest.put("trades", createTradesList());
        backtest.put("metrics", createMetricsMock());
        backtest.put("equityCurve", createEquityCurveMock());
        return backtest;
    }

    private List<Map<String, Object>> createTradesList() {
        List<Map<String, Object>> trades = new ArrayList<>();
        Map<String, Object> trade = new HashMap<>();
        trade.put("tradeId", "trade-1");
        trade.put("symbol", "005930");
        trade.put("side", "BUY");
        trade.put("quantity", 10);
        trade.put("price", new BigDecimal("70000"));
        trade.put("entryPrice", new BigDecimal("70000"));  // 템플릿에서 사용
        trade.put("exitPrice", new BigDecimal("75000"));   // 템플릿에서 사용
        trade.put("profitLoss", new BigDecimal("50000"));
        trade.put("returnPct", new BigDecimal("7.14"));    // 템플릿에서 사용
        trade.put("executedAt", "2024-01-15T10:00:00");
        trade.put("date", "2024-01-15");                   // backtest-detail.html에서 사용
        trade.put("entryTime", "2024-01-15T10:00:00");     // 템플릿에서 사용
        trade.put("exitTime", "2024-01-16T14:00:00");      // 템플릿에서 사용
        trades.add(trade);
        return trades;
    }

    private Map<String, Object> createMetricsMock() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalReturn", 0.15);
        metrics.put("annualizedReturn", 0.30);
        metrics.put("maxDrawdown", -0.05);
        metrics.put("sharpeRatio", 1.8);
        metrics.put("sortinoRatio", 2.1);
        metrics.put("winRate", 65.0);   // 템플릿에서 퍼센트로 표시
        metrics.put("profitFactor", 2.5);
        metrics.put("totalProfitLoss", new BigDecimal("1500000"));  // 템플릿에서 사용
        metrics.put("totalTrades", 50);                              // 템플릿에서 사용
        metrics.put("winningTrades", 32);                            // 템플릿에서 사용
        metrics.put("losingTrades", 18);                             // 템플릿에서 사용
        return metrics;
    }

    private List<Map<String, Object>> createEquityCurveMock() {
        List<Map<String, Object>> curve = new ArrayList<>();
        curve.add(Map.of("date", "2024-01-01", "equity", 10000000));
        curve.add(Map.of("date", "2024-03-01", "equity", 10500000));
        curve.add(Map.of("date", "2024-06-30", "equity", 11500000));
        return curve;
    }
}
