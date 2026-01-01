package com.maru.trading.controller;

import com.maru.strategy.service.StrategyService;
import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 전략 실행 히스토리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/execution-history")
@RequiredArgsConstructor
public class ExecutionHistoryController {

    private final TradingApiService tradingApiService;
    private final StrategyService strategyService;

    /**
     * 실행 히스토리 페이지
     */
    @GetMapping
    public String page(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            Model model) {

        try {
            log.info("Loading execution history page: strategyId={}, startDate={}, endDate={}, status={}",
                    strategyId, startDate, endDate, status);

            // 기본값 설정 (오늘부터 최근 30일)
            if (startDate == null || startDate.isEmpty()) {
                startDate = LocalDate.now().minusDays(30).toString();
            }
            if (endDate == null || endDate.isEmpty()) {
                endDate = LocalDate.now().toString();
            }

            // 전략 목록 조회 (필터용)
            List<com.maru.strategy.entity.Strategy> strategies = strategyService.getAllStrategies();
            model.addAttribute("strategies", strategies);

            // 실행 히스토리 조회
            Map<String, Object> result = tradingApiService.getExecutionHistory(
                    strategyId, startDate, endDate, status);

            // 결과를 모델에 추가
            model.addAttribute("executions", result.get("executions"));
            model.addAttribute("totalExecutions", result.get("totalExecutions"));
            model.addAttribute("successfulExecutions", result.get("successfulExecutions"));
            model.addAttribute("failedExecutions", result.get("failedExecutions"));
            model.addAttribute("totalProfitLoss", result.get("totalProfitLoss"));

            // 필터 값 유지
            model.addAttribute("strategyId", strategyId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("status", status);

            return "trading/execution-history";

        } catch (Exception e) {
            log.error("Failed to load execution history page", e);
            model.addAttribute("error", "실행 히스토리를 불러오는데 실패했습니다: " + e.getMessage());

            // 전략 목록만이라도 로드
            try {
                List<com.maru.strategy.entity.Strategy> strategies = strategyService.getAllStrategies();
                model.addAttribute("strategies", strategies);
            } catch (Exception ex) {
                log.error("Failed to load strategies", ex);
            }

            return "trading/execution-history";
        }
    }
}
