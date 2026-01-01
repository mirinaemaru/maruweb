package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * 데모 백테스트 및 최적화 데모 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/demo")
@RequiredArgsConstructor
public class DemoBacktestController {

    private final TradingApiService tradingApiService;

    // ==================== 최적화 데모 (Optimization Demo) ====================

    /**
     * 최적화 데모 페이지
     */
    @GetMapping("/optimization")
    public String optimizationPage(Model model) {
        return "trading/optimization-demo";
    }

    /**
     * MA 크로스오버 최적화 실행
     */
    @PostMapping("/optimization/ma-crossover")
    @ResponseBody
    public Map<String, Object> runMACrossoverOptimization() {
        log.info("Running MA Crossover optimization demo");
        return tradingApiService.runMACrossoverOptimization();
    }

    /**
     * RSI 최적화 실행
     */
    @PostMapping("/optimization/rsi")
    @ResponseBody
    public Map<String, Object> runRSIOptimization() {
        log.info("Running RSI optimization demo");
        return tradingApiService.runRSIOptimization();
    }

    /**
     * 랜덤 서치 최적화 실행
     */
    @PostMapping("/optimization/random-search")
    @ResponseBody
    public Map<String, Object> runRandomSearchOptimization() {
        log.info("Running Random Search optimization demo");
        return tradingApiService.runRandomSearchOptimization();
    }

    // ==================== 데모 백테스트 (Demo Backtest) ====================

    /**
     * 데모 백테스트 페이지
     */
    @GetMapping("/backtest")
    public String backtestDemoPage(Model model) {
        return "trading/demo-backtest";
    }

    /**
     * 데모 데이터 생성
     */
    @PostMapping("/backtest/generate-data")
    @ResponseBody
    public Map<String, Object> generateDemoData() {
        log.info("Generating demo data");
        return tradingApiService.generateDemoData();
    }

    /**
     * MA 크로스오버 백테스트 데모
     */
    @PostMapping("/backtest/ma-crossover")
    @ResponseBody
    public Map<String, Object> runMACrossoverBacktest() {
        log.info("Running MA Crossover backtest demo");
        return tradingApiService.runMACrossoverBacktest();
    }

    /**
     * RSI 백테스트 데모
     */
    @PostMapping("/backtest/rsi")
    @ResponseBody
    public Map<String, Object> runRSIBacktest() {
        log.info("Running RSI backtest demo");
        return tradingApiService.runRSIBacktest();
    }

    /**
     * 백테스트 비교 데모
     */
    @PostMapping("/backtest/compare")
    @ResponseBody
    public Map<String, Object> runBacktestComparison() {
        log.info("Running backtest comparison demo");
        return tradingApiService.runBacktestComparison();
    }

    /**
     * 데모 데이터 삭제
     */
    @DeleteMapping("/backtest/clear")
    @ResponseBody
    public Map<String, Object> clearDemoData() {
        log.info("Clearing demo data");
        return tradingApiService.clearDemoData();
    }
}
