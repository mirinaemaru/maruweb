package com.maru.trading.controller;

import com.maru.trading.controller.TestConfig;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BacktestController.class,
        properties = "spring.thymeleaf.enabled=false")
@ContextConfiguration(classes = TestConfig.class)
@Import(BacktestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BacktestController 단위 테스트")
class BacktestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @Test
    @DisplayName("백테스팅 결과 목록 페이지 - 성공")
    void list_Success() throws Exception {
        Map<String, Object> backtestResponse = new HashMap<>();
        Map<String, Object> backtest1 = new HashMap<>();
        backtest1.put("id", 1L);
        backtest1.put("strategyId", "strategy-001");
        backtest1.put("strategyName", "테스트 전략");
        backtest1.put("symbol", "005930");
        backtest1.put("startDate", "2025-01-01");
        backtest1.put("endDate", "2025-12-31");
        backtest1.put("status", "COMPLETED");
        backtest1.put("totalReturn", 0.15);
        backtest1.put("winRate", 0.65);
        backtest1.put("maxDrawdown", -0.10);
        backtest1.put("sharpeRatio", 1.5);
        backtest1.put("totalTrades", 50);
        backtest1.put("totalProfitLoss", 1500000);
        backtest1.put("createdAt", "2025-01-01T10:00:00");
        backtestResponse.put("backtests", Arrays.asList(backtest1));

        when(tradingApiService.getBacktestResults(any(), any(), any())).thenReturn(backtestResponse);

        mockMvc.perform(get("/trading/backtests")
                        .param("strategyId", "strategy-001")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests"))
                .andExpect(model().attributeExists("backtests"));
    }

    @Test
    @DisplayName("백테스팅 결과 목록 - API 오류")
    void list_ApiError() throws Exception {
        when(tradingApiService.getBacktestResults(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/backtests"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtests"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("백테스팅 결과 상세 페이지 - 성공")
    void detail_Success() throws Exception {
        Map<String, Object> backtestDetail = new HashMap<>();
        backtestDetail.put("id", 1L);
        backtestDetail.put("strategyId", "strategy-001");
        backtestDetail.put("strategyName", "테스트 전략");
        backtestDetail.put("symbol", "005930");
        backtestDetail.put("startDate", "2025-01-01");
        backtestDetail.put("endDate", "2025-12-31");
        backtestDetail.put("status", "COMPLETED");
        backtestDetail.put("initialCapital", 10000000);
        backtestDetail.put("finalCapital", 11500000);
        backtestDetail.put("totalReturn", 0.15);
        backtestDetail.put("winRate", 0.65);
        backtestDetail.put("maxDrawdown", -0.10);
        backtestDetail.put("sharpeRatio", 1.5);
        backtestDetail.put("totalTrades", 50);
        backtestDetail.put("trades", Arrays.asList());

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalReturn", 0.15);
        metrics.put("sharpeRatio", 1.5);
        metrics.put("maxDrawdown", -0.10);
        metrics.put("totalProfitLoss", 1500000);
        metrics.put("totalTrades", 50);
        metrics.put("winningTrades", 33);
        metrics.put("losingTrades", 17);
        metrics.put("winRate", 0.65);
        metrics.put("profitFactor", 2.5);
        metrics.put("avgWin", 30000);
        metrics.put("avgLoss", -15000);
        metrics.put("maxConsecutiveWins", 5);
        metrics.put("maxConsecutiveLosses", 3);
        backtestDetail.put("metrics", metrics);
        backtestDetail.put("equityCurve", Arrays.asList());

        when(tradingApiService.getBacktestDetail(1L)).thenReturn(backtestDetail);

        mockMvc.perform(get("/trading/backtests/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/backtest-detail"))
                .andExpect(model().attributeExists("backtest"))
                .andExpect(model().attributeExists("trades"));
    }

    @Test
    @DisplayName("Walk-Forward Analysis 페이지")
    void walkForwardPage() throws Exception {
        mockMvc.perform(get("/trading/backtests/advanced/walk-forward"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/walk-forward-analysis"));
    }

    @Test
    @DisplayName("Walk-Forward Analysis 실행 - API 오류시 분석 페이지로 복귀")
    void runWalkForward_ApiError() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("error", "API 연결 실패");

        when(tradingApiService.runWalkForwardAnalysis(any())).thenReturn(result);

        mockMvc.perform(post("/trading/backtests/advanced/walk-forward")
                        .param("strategyId", "strategy-001")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31")
                        .param("windowDays", "30")
                        .param("stepDays", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/walk-forward-analysis"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("Portfolio Backtest 페이지")
    void portfolioBacktestPage() throws Exception {
        mockMvc.perform(get("/trading/backtests/advanced/portfolio"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/portfolio-backtest"));
    }
}
