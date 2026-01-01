package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 백테스팅 결과 조회 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/backtests")
@RequiredArgsConstructor
public class BacktestController {

    private final TradingApiService tradingApiService;

    /**
     * 백테스팅 결과 목록 페이지
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            log.info("Loading backtest results page: strategyId={}, startDate={}, endDate={}",
                    strategyId, startDate, endDate);

            // 백테스팅 결과 목록 조회
            Map<String, Object> result = tradingApiService.getBacktestResults(strategyId, startDate, endDate);

            model.addAttribute("backtests", result.get("backtests"));
            model.addAttribute("strategyId", strategyId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "trading/backtests";

        } catch (Exception e) {
            log.error("Failed to load backtest results", e);
            model.addAttribute("error", "백테스팅 결과를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/backtests";
        }
    }

    /**
     * 백테스팅 결과 상세 페이지
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            log.info("Loading backtest detail: id={}", id);

            // 백테스팅 결과 상세 조회
            Map<String, Object> backtest = tradingApiService.getBacktestDetail(id);

            model.addAttribute("backtest", backtest);
            model.addAttribute("trades", backtest.get("trades"));
            model.addAttribute("metrics", backtest.get("metrics"));
            model.addAttribute("equityCurve", backtest.get("equityCurve"));

            return "trading/backtest-detail";

        } catch (Exception e) {
            log.error("Failed to load backtest detail", e);
            model.addAttribute("error", "백테스팅 상세 결과를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/backtest-detail";
        }
    }

    /**
     * Walk-Forward Analysis 페이지
     */
    @GetMapping("/advanced/walk-forward")
    public String walkForwardPage(Model model) {
        return "trading/walk-forward-analysis";
    }

    /**
     * Walk-Forward Analysis 실행
     */
    @PostMapping("/advanced/walk-forward")
    public String runWalkForward(
            @RequestParam String strategyId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false, defaultValue = "30") int windowDays,
            @RequestParam(required = false, defaultValue = "7") int stepDays,
            @RequestParam(required = false, defaultValue = "10000000") long initialCapital,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            log.info("Running Walk-Forward Analysis: strategy={}, period={} to {}", strategyId, startDate, endDate);

            Map<String, Object> request = Map.of(
                "strategyId", strategyId,
                "startDate", startDate,
                "endDate", endDate,
                "windowDays", windowDays,
                "stepDays", stepDays,
                "initialCapital", initialCapital
            );

            Map<String, Object> result = tradingApiService.runWalkForwardAnalysis(request);

            if (result.containsKey("error")) {
                model.addAttribute("error", result.get("error"));
                return "trading/walk-forward-analysis";
            }

            model.addAttribute("result", result);
            model.addAttribute("strategyId", strategyId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "trading/walk-forward-result";

        } catch (Exception e) {
            log.error("Failed to run walk-forward analysis", e);
            model.addAttribute("error", "Walk-Forward Analysis 실행 실패: " + e.getMessage());
            return "trading/walk-forward-analysis";
        }
    }

    /**
     * Portfolio Backtest 페이지
     */
    @GetMapping("/advanced/portfolio")
    public String portfolioBacktestPage(Model model) {
        return "trading/portfolio-backtest";
    }

    /**
     * Portfolio Backtest 실행
     */
    @PostMapping("/advanced/portfolio")
    public String runPortfolioBacktest(
            @RequestParam String symbols,  // Comma-separated
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false, defaultValue = "10000000") long initialCapital,
            @RequestParam(required = false, defaultValue = "EQUAL") String allocationType,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            log.info("Running Portfolio Backtest: symbols={}, period={} to {}", symbols, startDate, endDate);

            Map<String, Object> request = Map.of(
                "symbols", List.of(symbols.split(",")),
                "startDate", startDate,
                "endDate", endDate,
                "initialCapital", initialCapital,
                "allocationType", allocationType
            );

            Map<String, Object> result = tradingApiService.runPortfolioBacktest(request);

            if (result.containsKey("error")) {
                model.addAttribute("error", result.get("error"));
                return "trading/portfolio-backtest";
            }

            model.addAttribute("result", result);
            model.addAttribute("symbols", symbols);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "trading/portfolio-backtest-result";

        } catch (Exception e) {
            log.error("Failed to run portfolio backtest", e);
            model.addAttribute("error", "Portfolio Backtest 실행 실패: " + e.getMessage());
            return "trading/portfolio-backtest";
        }
    }

    /**
     * 백테스트 거래 상세 조회
     */
    @GetMapping("/{id}/trades")
    public String backtestTrades(@PathVariable Long id, Model model) {
        try {
            log.info("Loading backtest trades: id={}", id);

            // 백테스트 기본 정보 조회
            Map<String, Object> backtest = tradingApiService.getBacktestDetail(id);

            // 거래 상세 조회
            Map<String, Object> tradesResult = tradingApiService.getBacktestTrades(id);

            model.addAttribute("backtest", backtest);
            model.addAttribute("trades", tradesResult.get("trades"));

            return "trading/backtest-trades";

        } catch (Exception e) {
            log.error("Failed to load backtest trades", e);
            model.addAttribute("error", "백테스트 거래 내역을 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/backtest-trades";
        }
    }
}
