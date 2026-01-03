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

    // ==================== Admin Backtest ====================

    /**
     * 백테스트 관리 페이지 (Admin)
     */
    @GetMapping("/admin")
    public String adminList(Model model) {
        try {
            log.info("Loading admin backtest list");

            try {
                Map<String, Object> result = tradingApiService.listBacktests();

                model.addAttribute("backtests", result.get("backtests"));
                model.addAttribute("error", result.get("error"));
            } catch (Exception apiException) {
                log.warn("Failed to load backtests from Trading System API (timeout or unavailable): {}",
                        apiException.getMessage());
                // API 타임아웃 시 빈 결과와 경고 메시지 표시
                model.addAttribute("backtests", new java.util.ArrayList<>());
                model.addAttribute("warning", "Trading System API 응답 시간이 초과되었습니다. 나중에 다시 시도해주세요.");
            }

            return "trading/backtests-admin";

        } catch (Exception e) {
            log.error("Failed to load admin backtest list", e);
            model.addAttribute("error", "백테스트 목록을 불러오는데 실패했습니다: " + e.getMessage());
            model.addAttribute("backtests", new java.util.ArrayList<>());
            return "trading/backtests-admin";
        }
    }

    /**
     * 백테스트 실행 페이지
     */
    @GetMapping("/admin/run")
    public String runBacktestPage(Model model) {
        return "trading/backtest-run";
    }

    /**
     * 백테스트 실행
     */
    @PostMapping("/admin/run")
    public String runBacktest(
            @RequestParam String strategyId,
            @RequestParam String symbols,  // Comma-separated
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false, defaultValue = "1d") String timeframe,
            @RequestParam(required = false, defaultValue = "10000000") long initialCapital,
            @RequestParam(required = false, defaultValue = "0.0015") double commission,
            @RequestParam(required = false, defaultValue = "0.0005") double slippage,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            log.info("Running backtest: strategy={}, symbols={}, period={} to {}",
                    strategyId, symbols, startDate, endDate);

            Map<String, Object> request = new java.util.HashMap<>();
            request.put("strategyId", strategyId);
            request.put("symbols", List.of(symbols.split(",")));
            request.put("startDate", startDate);
            request.put("endDate", endDate);
            request.put("timeframe", timeframe);
            request.put("initialCapital", new java.math.BigDecimal(initialCapital));
            request.put("commission", new java.math.BigDecimal(commission));
            request.put("slippage", new java.math.BigDecimal(slippage));

            Map<String, Object> result = tradingApiService.runBacktest(request);

            if (result.containsKey("error")) {
                redirectAttributes.addFlashAttribute("errorMessage", result.get("error"));
                return "redirect:/trading/backtests/admin/run";
            }

            redirectAttributes.addFlashAttribute("successMessage", "백테스트가 성공적으로 실행되었습니다.");
            return "redirect:/trading/backtests/admin";

        } catch (Exception e) {
            log.error("Failed to run backtest", e);
            redirectAttributes.addFlashAttribute("errorMessage", "백테스트 실행 실패: " + e.getMessage());
            return "redirect:/trading/backtests/admin/run";
        }
    }

    /**
     * 백테스트 삭제
     */
    @PostMapping("/admin/{backtestId}/delete")
    public String deleteBacktest(@PathVariable String backtestId, RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting backtest: {}", backtestId);

            Map<String, Object> result = tradingApiService.deleteBacktest(backtestId);

            if (result.containsKey("error")) {
                redirectAttributes.addFlashAttribute("errorMessage", result.get("error"));
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "백테스트가 삭제되었습니다.");
            }

        } catch (Exception e) {
            log.error("Failed to delete backtest", e);
            redirectAttributes.addFlashAttribute("errorMessage", "백테스트 삭제 실패: " + e.getMessage());
        }

        return "redirect:/trading/backtests/admin";
    }

    /**
     * 백테스트 상세 (Admin)
     */
    @GetMapping("/admin/{backtestId}")
    public String adminDetail(@PathVariable String backtestId, Model model) {
        try {
            log.info("Loading admin backtest detail: {}", backtestId);

            Map<String, Object> backtest = tradingApiService.getBacktest(backtestId);

            if (backtest.containsKey("error")) {
                model.addAttribute("error", backtest.get("error"));
            }

            model.addAttribute("backtest", backtest);

            return "trading/backtest-admin-detail";

        } catch (Exception e) {
            log.error("Failed to load admin backtest detail", e);
            model.addAttribute("error", "백테스트 상세 정보를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/backtest-admin-detail";
        }
    }
}
