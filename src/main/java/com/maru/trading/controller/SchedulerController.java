package com.maru.trading.controller;

import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.SchedulerStatusResponse;
import com.maru.trading.service.SchedulerService;
import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 전략 스케줄러 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;
    private final TradingApiService tradingApiService;

    /**
     * 스케줄러 관리 페이지
     */
    @GetMapping
    public String schedulerPage(Model model) {
        log.info("GET /trading/scheduler - Scheduler management page");

        try {
            // 스케줄러 상태 조회
            SchedulerStatusResponse statusResponse = schedulerService.getStatus();
            model.addAttribute("status", statusResponse);

            // 활성 전략 목록 조회 (전략 선택 드롭다운용)
            List<Map<String, Object>> strategies = loadActiveStrategies();
            model.addAttribute("strategies", strategies);

            log.info("Scheduler page loaded: enabled={}, status={}, activeStrategies={}",
                    statusResponse.isEnabled(),
                    statusResponse.getStatus(),
                    statusResponse.getActiveStrategiesCount());

        } catch (Exception e) {
            log.error("Failed to load scheduler page", e);
            model.addAttribute("error", "스케줄러 정보를 불러오는 데 실패했습니다: " + e.getMessage());
        }

        return "trading/scheduler";
    }

    /**
     * 스케줄러 활성화
     */
    @PostMapping("/enable")
    public String enableScheduler(RedirectAttributes redirectAttributes) {
        log.info("POST /trading/scheduler/enable - Enabling scheduler");

        try {
            AckResponse response = schedulerService.enable();

            if (response != null && Boolean.TRUE.equals(response.getOk())) {
                redirectAttributes.addFlashAttribute("message", "스케줄러가 활성화되었습니다.");
                log.info("Successfully enabled scheduler");
            } else {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                redirectAttributes.addFlashAttribute("error", "스케줄러 활성화 실패: " + errorMsg);
                log.warn("Failed to enable scheduler: {}", errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to enable scheduler", e);
            redirectAttributes.addFlashAttribute("error", "스케줄러 활성화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/scheduler";
    }

    /**
     * 스케줄러 비활성화
     */
    @PostMapping("/disable")
    public String disableScheduler(RedirectAttributes redirectAttributes) {
        log.info("POST /trading/scheduler/disable - Disabling scheduler");

        try {
            AckResponse response = schedulerService.disable();

            if (response != null && Boolean.TRUE.equals(response.getOk())) {
                redirectAttributes.addFlashAttribute("message", "스케줄러가 비활성화되었습니다.");
                log.info("Successfully disabled scheduler");
            } else {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                redirectAttributes.addFlashAttribute("error", "스케줄러 비활성화 실패: " + errorMsg);
                log.warn("Failed to disable scheduler: {}", errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to disable scheduler", e);
            redirectAttributes.addFlashAttribute("error", "스케줄러 비활성화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/scheduler";
    }

    /**
     * 특정 전략 수동 실행
     */
    @PostMapping("/trigger")
    public String triggerStrategy(
            @RequestParam(required = false) String strategyId,
            RedirectAttributes redirectAttributes
    ) {
        log.info("POST /trading/scheduler/trigger - strategyId={}", strategyId);

        try {
            // 입력 검증
            if (strategyId == null || strategyId.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "실행할 전략을 선택해주세요.");
                return "redirect:/trading/scheduler";
            }

            AckResponse response = schedulerService.trigger(strategyId.trim());

            if (response != null && Boolean.TRUE.equals(response.getOk())) {
                redirectAttributes.addFlashAttribute("message", "전략이 실행되었습니다. (ID: " + strategyId + ")");
                log.info("Successfully triggered strategy: {}", strategyId);
            } else {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                redirectAttributes.addFlashAttribute("error", "전략 실행 실패: " + errorMsg);
                log.warn("Failed to trigger strategy {}: {}", strategyId, errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to trigger strategy: {}", strategyId, e);
            redirectAttributes.addFlashAttribute("error", "전략 실행 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/scheduler";
    }

    /**
     * 모든 활성 전략 즉시 실행
     */
    @PostMapping("/execute-all")
    public String executeAllStrategies(RedirectAttributes redirectAttributes) {
        log.info("POST /trading/scheduler/execute-all - Executing all active strategies");

        try {
            AckResponse response = schedulerService.executeAll();

            if (response != null && Boolean.TRUE.equals(response.getOk())) {
                redirectAttributes.addFlashAttribute("message", "모든 활성 전략이 실행되었습니다.");
                log.info("Successfully executed all strategies");
            } else {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                redirectAttributes.addFlashAttribute("error", "전체 실행 실패: " + errorMsg);
                log.warn("Failed to execute all strategies: {}", errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to execute all strategies", e);
            redirectAttributes.addFlashAttribute("error", "전체 실행 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/scheduler";
    }

    /**
     * 실행 통계 초기화
     */
    @PostMapping("/reset-stats")
    public String resetStats(RedirectAttributes redirectAttributes) {
        log.info("POST /trading/scheduler/reset-stats - Resetting scheduler stats");

        try {
            AckResponse response = schedulerService.resetStats();

            if (response != null && Boolean.TRUE.equals(response.getOk())) {
                redirectAttributes.addFlashAttribute("message", "통계가 초기화되었습니다.");
                log.info("Successfully reset scheduler stats");
            } else {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                redirectAttributes.addFlashAttribute("error", "통계 초기화 실패: " + errorMsg);
                log.warn("Failed to reset stats: {}", errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to reset stats", e);
            redirectAttributes.addFlashAttribute("error", "통계 초기화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/scheduler";
    }

    /**
     * 활성 전략 목록 로드
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadActiveStrategies() {
        try {
            Map<String, Object> strategiesResponse = tradingApiService.getStrategies();
            if (strategiesResponse != null && strategiesResponse.containsKey("strategies")) {
                Object strategiesObj = strategiesResponse.get("strategies");
                if (strategiesObj instanceof List) {
                    return (List<Map<String, Object>>) strategiesObj;
                }
            }
            // 응답이 리스트 형태일 수도 있음
            if (strategiesResponse != null && strategiesResponse.containsKey("items")) {
                Object itemsObj = strategiesResponse.get("items");
                if (itemsObj instanceof List) {
                    return (List<Map<String, Object>>) itemsObj;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load strategies: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}
