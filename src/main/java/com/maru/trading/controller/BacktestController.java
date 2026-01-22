package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

            // API 응답에서 FAILED 상태 체크
            String status = (String) result.get("status");
            if ("FAILED".equals(status)) {
                String errorMsg = (String) result.get("errorMessage");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    // 사용자 친화적 메시지로 변환
                    String friendlyMsg = extractFriendlyErrorMessage(errorMsg);
                    redirectAttributes.addFlashAttribute("errorMessage", "백테스트 실행 실패: " + friendlyMsg);
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "백테스트 실행에 실패했습니다. 입력값을 확인해주세요.");
                }
                return "redirect:/trading/backtests/admin/run";
            }

            redirectAttributes.addFlashAttribute("successMessage", "백테스트가 성공적으로 실행되었습니다.");
            return "redirect:/trading/backtests/admin";

        } catch (Exception e) {
            log.error("Failed to run backtest", e);
            // 사용자 친화적 에러 메시지 추출
            String errorMsg = extractFriendlyErrorMessage(e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "백테스트 실행 실패: " + errorMsg);
            return "redirect:/trading/backtests/admin/run";
        }
    }

    /**
     * 사용자 친화적 에러 메시지 추출
     */
    private String extractFriendlyErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return "알 수 없는 오류가 발생했습니다.";
        }

        // JSON 응답에서 errorMessage 필드 추출 시도
        if (rawMessage.contains("\"errorMessage\":")) {
            try {
                int start = rawMessage.indexOf("\"errorMessage\":\"") + 16;
                int end = rawMessage.indexOf("\"", start);
                if (start > 16 && end > start) {
                    String extracted = rawMessage.substring(start, end);
                    // Backtest execution failed: 접두사 제거
                    if (extracted.startsWith("Backtest execution failed: ")) {
                        extracted = extracted.substring(27);
                    }
                    // Java 예외 메시지 단순화
                    if (extracted.contains("Cannot invoke")) {
                        return "백테스트 데이터 처리 중 오류가 발생했습니다. 전략 설정을 확인해주세요.";
                    }
                    return extracted;
                }
            } catch (Exception ignored) {
                // 파싱 실패 시 원본 메시지 처리 계속
            }
        }

        // HTTP 상태 코드 관련 메시지 처리
        if (rawMessage.contains("400")) {
            if (rawMessage.contains("Cannot invoke") || rawMessage.contains("NullPointerException")) {
                return "백테스트 데이터 처리 중 오류가 발생했습니다. 전략 설정을 확인해주세요.";
            }
            return "잘못된 요청입니다. 입력값을 확인해주세요.";
        }
        if (rawMessage.contains("404")) {
            return "전략 또는 종목을 찾을 수 없습니다.";
        }
        if (rawMessage.contains("500")) {
            return "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
        if (rawMessage.contains("Connection refused") || rawMessage.contains("timeout")) {
            return "Trading System에 연결할 수 없습니다.";
        }

        // 메시지가 너무 길면 자르기
        if (rawMessage.length() > 100) {
            return rawMessage.substring(0, 100) + "...";
        }

        return rawMessage;
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

    // ==================== 비동기 백테스트 (Async Backtest) ====================

    /**
     * 비동기 백테스트 실행 페이지
     */
    @GetMapping("/admin/async")
    public String asyncBacktestPage(Model model) {
        return "trading/backtest-async";
    }

    /**
     * 비동기 백테스트 제출 (AJAX)
     */
    @PostMapping("/admin/async")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAsyncBacktest(
            @RequestParam String strategyId,
            @RequestParam String symbols,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false, defaultValue = "1d") String timeframe,
            @RequestParam(required = false, defaultValue = "10000000") long initialCapital,
            @RequestParam(required = false, defaultValue = "0.0015") double commission,
            @RequestParam(required = false, defaultValue = "0.0005") double slippage) {

        try {
            log.info("Submitting async backtest: strategy={}, symbols={}, period={} to {}",
                    strategyId, symbols, startDate, endDate);

            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", strategyId);
            request.put("symbols", List.of(symbols.split(",")));
            request.put("startDate", startDate);
            request.put("endDate", endDate);
            request.put("timeframe", timeframe);
            request.put("initialCapital", new BigDecimal(initialCapital));
            request.put("commission", new BigDecimal(commission));
            request.put("slippage", new BigDecimal(slippage));

            Map<String, Object> result = tradingApiService.submitAsyncBacktest(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to submit async backtest", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "비동기 백테스트 제출 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 작업 상태 조회 (AJAX)
     */
    @GetMapping("/admin/jobs/{jobId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        try {
            Map<String, Object> status = tradingApiService.getAsyncBacktestStatus(jobId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get job status", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "작업 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * SSE 진행률 스트리밍
     */
    @GetMapping(value = "/admin/jobs/{jobId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobProgress(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Map<String, Object> status = tradingApiService.getAsyncBacktestStatus(jobId);

                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(status));

                    String jobStatus = (String) status.get("status");
                    if ("COMPLETED".equals(jobStatus) || "FAILED".equals(jobStatus) || "CANCELLED".equals(jobStatus)) {
                        emitter.complete();
                        break;
                    }

                    Thread.sleep(1000); // 1초마다 폴링
                }
            } catch (IOException e) {
                log.warn("SSE connection closed by client for job: {}", jobId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error streaming job progress for job: {}", jobId, e);
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(executor::shutdown);
        emitter.onError(e -> executor.shutdown());

        return emitter;
    }

    /**
     * 작업 취소 (AJAX)
     */
    @PostMapping("/admin/jobs/{jobId}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        try {
            log.info("Cancelling job: {}", jobId);
            Map<String, Object> result = tradingApiService.cancelAsyncBacktest(jobId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to cancel job", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "작업 취소 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 작업 결과 페이지
     */
    @GetMapping("/admin/jobs/{jobId}/result")
    public String jobResultPage(@PathVariable String jobId, Model model) {
        try {
            log.info("Loading job result page: {}", jobId);

            Map<String, Object> result = tradingApiService.getAsyncBacktestResult(jobId);

            if (result.containsKey("error")) {
                model.addAttribute("error", result.get("error"));
            }

            model.addAttribute("jobId", jobId);
            model.addAttribute("result", result);

            return "trading/backtest-job-result";

        } catch (Exception e) {
            log.error("Failed to load job result page", e);
            model.addAttribute("error", "작업 결과를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/backtest-job-result";
        }
    }

    // ==================== 몬테카를로 시뮬레이션 (Monte Carlo Simulation) ====================

    /**
     * 몬테카를로 시뮬레이션 페이지
     */
    @GetMapping("/admin/{backtestId}/monte-carlo")
    public String monteCarloPage(@PathVariable String backtestId, Model model) {
        try {
            log.info("Loading Monte Carlo page for backtest: {}", backtestId);

            Map<String, Object> backtest = tradingApiService.getBacktest(backtestId);

            if (backtest.containsKey("error")) {
                model.addAttribute("error", backtest.get("error"));
            }

            model.addAttribute("backtest", backtest);
            model.addAttribute("backtestId", backtestId);

            return "trading/monte-carlo";

        } catch (Exception e) {
            log.error("Failed to load Monte Carlo page", e);
            model.addAttribute("error", "몬테카를로 페이지를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/monte-carlo";
        }
    }

    /**
     * 몬테카를로 시뮬레이션 실행 (AJAX)
     */
    @PostMapping("/admin/{backtestId}/monte-carlo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runMonteCarloSimulation(
            @PathVariable String backtestId,
            @RequestParam(required = false, defaultValue = "1000") int numSimulations,
            @RequestParam(required = false, defaultValue = "BOOTSTRAP") String method,
            @RequestParam(required = false, defaultValue = "95") int confidenceLevel) {

        try {
            log.info("Running Monte Carlo simulation for backtest: {}, simulations={}, method={}, confidence={}",
                    backtestId, numSimulations, method, confidenceLevel);

            Map<String, Object> request = new HashMap<>();
            request.put("backtestId", backtestId);
            request.put("numSimulations", numSimulations);
            request.put("method", method);
            request.put("confidenceLevel", confidenceLevel);

            Map<String, Object> result = tradingApiService.runMonteCarloSimulation(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to run Monte Carlo simulation", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "몬테카를로 시뮬레이션 실행 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}
