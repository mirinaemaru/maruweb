package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo Scenarios Controller
 * 6가지 사전정의된 데모 시나리오 실행 페이지
 */
@Slf4j
@Controller
@RequestMapping("/trading/demo-scenarios")
@RequiredArgsConstructor
public class DemoController {

    private final TradingApiService tradingApiService;

    /**
     * 데모 시나리오 목록 페이지
     */
    @GetMapping
    public String page(Model model,
                      @RequestParam(required = false) String successMessage,
                      @RequestParam(required = false) String errorMessage) {
        try {
            log.info("Loading Demo Scenarios page");

            // 메시지가 있으면 모델에 추가
            if (successMessage != null) {
                model.addAttribute("message", successMessage);
            }
            if (errorMessage != null) {
                model.addAttribute("error", errorMessage);
            }

            model.addAttribute("apiConnected", true);
            return "trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to load demo scenarios page", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/demo-scenarios";
        }
    }

    /**
     * Golden Cross 시나리오 실행
     */
    @PostMapping("/golden-cross")
    public String runGoldenCross(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running Golden Cross scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runGoldenCrossDemo(params);

            String resultMessage = String.format(
                "Golden Cross 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run Golden Cross scenario", e);
            redirectAttributes.addFlashAttribute("error", "Golden Cross 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * Death Cross 시나리오 실행
     */
    @PostMapping("/death-cross")
    public String runDeathCross(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running Death Cross scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runDeathCrossDemo(params);

            String resultMessage = String.format(
                "Death Cross 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run Death Cross scenario", e);
            redirectAttributes.addFlashAttribute("error", "Death Cross 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * RSI Oversold 시나리오 실행
     */
    @PostMapping("/rsi-oversold")
    public String runRsiOversold(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running RSI Oversold scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runRsiOversoldDemo(params);

            String resultMessage = String.format(
                "RSI Oversold 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run RSI Oversold scenario", e);
            redirectAttributes.addFlashAttribute("error", "RSI Oversold 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * RSI Overbought 시나리오 실행
     */
    @PostMapping("/rsi-overbought")
    public String runRsiOverbought(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running RSI Overbought scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runRsiOverboughtDemo(params);

            String resultMessage = String.format(
                "RSI Overbought 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run RSI Overbought scenario", e);
            redirectAttributes.addFlashAttribute("error", "RSI Overbought 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * Volatile 시나리오 실행
     */
    @PostMapping("/volatile")
    public String runVolatile(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running Volatile scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runVolatileDemo(params);

            String resultMessage = String.format(
                "Volatile 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run Volatile scenario", e);
            redirectAttributes.addFlashAttribute("error", "Volatile 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * Stable 시나리오 실행
     */
    @PostMapping("/stable")
    public String runStable(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running Stable scenario: accountId={}, symbol={}", accountId, symbol);

            Map<String, Object> params = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                params.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            Map<String, Object> result = tradingApiService.runStableDemo(params);

            String resultMessage = String.format(
                "Stable 시나리오가 성공적으로 실행되었습니다. | 결과: %s",
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run Stable scenario", e);
            redirectAttributes.addFlashAttribute("error", "Stable 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }

    /**
     * 커스텀 데모 시나리오 실행
     */
    @PostMapping("/custom")
    public String runCustom(
            @RequestParam String scenarioName,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "10") Integer tradeCount,
            @RequestParam(defaultValue = "MIXED") String scenarioType,
            @RequestParam(defaultValue = "50000") Integer minPrice,
            @RequestParam(defaultValue = "100000") Integer maxPrice,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Running Custom scenario: name={}, accountId={}, symbol={}, type={}, tradeCount={}",
                    scenarioName, accountId, symbol, scenarioType, tradeCount);

            // 커스텀 시나리오 파라미터 구성
            Map<String, Object> scenarioData = new HashMap<>();
            scenarioData.put("scenarioName", scenarioName);
            scenarioData.put("scenarioType", scenarioType);
            scenarioData.put("tradeCount", tradeCount);
            scenarioData.put("minPrice", minPrice);
            scenarioData.put("maxPrice", maxPrice);

            if (accountId != null && !accountId.isEmpty()) {
                scenarioData.put("accountId", accountId);
            }
            if (symbol != null && !symbol.isEmpty()) {
                scenarioData.put("symbol", symbol);
            }
            if (description != null && !description.isEmpty()) {
                scenarioData.put("description", description);
            }

            // 커스텀 데모 실행
            Map<String, Object> result = tradingApiService.runCustomDemo(scenarioData);

            String resultMessage = String.format(
                "커스텀 시나리오 '%s'가 성공적으로 실행되었습니다. | 유형: %s | 거래수: %d | 결과: %s",
                scenarioName, scenarioType, tradeCount,
                result.getOrDefault("message", result.toString())
            );
            redirectAttributes.addFlashAttribute("message", resultMessage);
            return "redirect:/trading/demo-scenarios";

        } catch (Exception e) {
            log.error("Failed to run Custom scenario", e);
            redirectAttributes.addFlashAttribute("error",
                    "커스텀 시나리오 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-scenarios";
        }
    }
}
