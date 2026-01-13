package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 전략 실행 히스토리 컨트롤러
 * 외부 Trading System API만 사용합니다.
 */
@Slf4j
@Controller
@RequestMapping("/trading/execution-history")
@RequiredArgsConstructor
public class ExecutionHistoryController {

    private final TradingApiService tradingApiService;

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

        // 기본값 설정 (오늘부터 최근 30일)
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusDays(30).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        // 필터 값 유지
        model.addAttribute("strategyId", strategyId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status);

        try {
            log.info("Loading execution history page: strategyId={}, startDate={}, endDate={}, status={}",
                    strategyId, startDate, endDate, status);

            // 전략 목록 조회 (필터용) - Trading System API에서 조회
            try {
                Map<String, Object> strategiesData = tradingApiService.getStrategies();
                List<?> strategies = (List<?>) strategiesData.get("items");
                model.addAttribute("strategies", strategies != null ? strategies : Collections.emptyList());
            } catch (Exception e) {
                log.warn("Failed to load strategies for filter", e);
                model.addAttribute("strategies", Collections.emptyList());
            }

            // 실행 히스토리 조회 - 타임아웃 발생 가능
            try {
                Map<String, Object> result = tradingApiService.getExecutionHistory(
                        strategyId, startDate, endDate, status);

                // 결과를 모델에 추가
                model.addAttribute("executions", result.get("executions"));
                model.addAttribute("totalExecutions", result.get("totalExecutions"));
                model.addAttribute("successfulExecutions", result.get("successfulExecutions"));
                model.addAttribute("failedExecutions", result.get("failedExecutions"));
                model.addAttribute("totalProfitLoss", result.get("totalProfitLoss"));
            } catch (Exception apiException) {
                log.warn("Failed to load execution history from Trading System API (timeout or unavailable): {}",
                        apiException.getMessage());
                // API 타임아웃 시 빈 결과와 경고 메시지 표시
                model.addAttribute("executions", Collections.emptyList());
                model.addAttribute("totalExecutions", 0);
                model.addAttribute("successfulExecutions", 0);
                model.addAttribute("failedExecutions", 0);
                model.addAttribute("totalProfitLoss", 0);
                model.addAttribute("warning", "Trading System API 응답 시간이 초과되었습니다. 검색 기간을 줄이거나 나중에 다시 시도해주세요.");
            }

            return "trading/execution-history";

        } catch (Exception e) {
            log.error("Failed to load execution history page", e);
            model.addAttribute("error", "실행 히스토리 페이지를 불러오는데 실패했습니다: " + e.getMessage());

            // 기본값 설정
            model.addAttribute("executions", Collections.emptyList());
            model.addAttribute("totalExecutions", 0);
            model.addAttribute("successfulExecutions", 0);
            model.addAttribute("failedExecutions", 0);
            model.addAttribute("totalProfitLoss", 0);

            return "trading/execution-history";
        }
    }
}
