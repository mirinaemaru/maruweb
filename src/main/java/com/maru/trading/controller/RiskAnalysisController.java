package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 리스크 분석 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/risk")
@RequiredArgsConstructor
public class RiskAnalysisController {

    private final TradingApiService tradingApiService;

    /**
     * VaR (Value at Risk) 분석 페이지
     */
    @GetMapping("/var")
    public String varAnalysis(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false, defaultValue = "0.95") double confidenceLevel,
            @RequestParam(required = false, defaultValue = "1") int timeHorizon,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            // 기본값 설정
            if (startDate == null) startDate = LocalDate.now().minusDays(30).toString();
            if (endDate == null) endDate = LocalDate.now().toString();

            log.info("Loading VaR analysis: strategyId={}, confidenceLevel={}, timeHorizon={}, startDate={}, endDate={}",
                    strategyId, confidenceLevel, timeHorizon, startDate, endDate);

            // VaR 계산
            Map<String, Object> varResult = tradingApiService.calculateVaR(
                strategyId, confidenceLevel, timeHorizon, startDate, endDate);

            model.addAttribute("strategyId", strategyId);
            model.addAttribute("confidenceLevel", confidenceLevel);
            model.addAttribute("timeHorizon", timeHorizon);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("varData", varResult.get("varData"));
            model.addAttribute("statistics", varResult.get("statistics"));

            return "trading/var-analysis";

        } catch (Exception e) {
            log.error("Failed to load VaR analysis", e);
            model.addAttribute("error", "VaR 분석을 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/var-analysis";
        }
    }

    /**
     * 상관관계 분석 페이지
     */
    @GetMapping("/correlation")
    public String correlationAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            // 기본값 설정
            if (startDate == null) startDate = LocalDate.now().minusDays(90).toString();
            if (endDate == null) endDate = LocalDate.now().toString();

            log.info("Loading correlation analysis: startDate={}, endDate={}", startDate, endDate);

            // 상관관계 분석
            Map<String, Object> correlationResult = tradingApiService.getCorrelationAnalysis(startDate, endDate);

            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("correlationMatrix", correlationResult.get("correlationMatrix"));
            model.addAttribute("strategies", correlationResult.get("strategies"));

            return "trading/correlation-analysis";

        } catch (Exception e) {
            log.error("Failed to load correlation analysis", e);
            model.addAttribute("error", "상관관계 분석을 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/correlation-analysis";
        }
    }

    /**
     * 리스크 대시보드 (통합 뷰)
     */
    @GetMapping("/dashboard")
    public String riskDashboard(Model model) {
        try {
            log.info("Loading risk dashboard");

            String endDate = LocalDate.now().toString();
            String startDate = LocalDate.now().minusDays(30).toString();

            // VaR, 상관관계 등 주요 리스크 지표 조회
            Map<String, Object> varResult = tradingApiService.calculateVaR(null, 0.95, 1, startDate, endDate);
            Map<String, Object> correlationResult = tradingApiService.getCorrelationAnalysis(startDate, endDate);

            model.addAttribute("varData", varResult.get("varData"));
            model.addAttribute("correlationMatrix", correlationResult.get("correlationMatrix"));
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "trading/risk-dashboard";

        } catch (Exception e) {
            log.error("Failed to load risk dashboard", e);
            model.addAttribute("error", "리스크 대시보드를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/risk-dashboard";
        }
    }
}
