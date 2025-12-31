package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo Signal Controller
 * 테스트용 수동 신호 주입 페이지
 */
@Slf4j
@Controller
@RequestMapping("/trading/demo-signal")
@RequiredArgsConstructor
public class DemoSignalController {

    private final TradingApiService tradingApiService;

    /**
     * 데모 신호 주입 페이지
     */
    @GetMapping
    public String page(Model model) {
        try {
            log.info("Loading Demo Signal page");

            // 계좌 목록 (선택용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);

            model.addAttribute("apiConnected", true);
            return "trading/demo-signal";

        } catch (Exception e) {
            log.error("Failed to load demo signal page", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/demo-signal";
        }
    }

    /**
     * 데모 신호 주입 처리
     */
    @PostMapping
    public String injectSignal(
            @RequestParam(required = false) String accountId,
            @RequestParam String symbol,
            @RequestParam String side,
            @RequestParam(required = false) String targetType,
            @RequestParam BigDecimal targetValue,
            @RequestParam(required = false) Integer ttlSeconds,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Injecting demo signal: accountId={}, symbol={}, side={}, targetType={}, targetValue={}, ttlSeconds={}",
                    accountId, symbol, side, targetType, targetValue, ttlSeconds);

            Map<String, Object> signalData = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) {
                signalData.put("accountId", accountId);
            }
            signalData.put("symbol", symbol);
            signalData.put("side", side);
            signalData.put("targetType", targetType != null && !targetType.isEmpty() ? targetType : "QTY");
            signalData.put("targetValue", targetValue);
            if (ttlSeconds != null) {
                signalData.put("ttlSeconds", ttlSeconds);
            }

            Map<String, Object> result = tradingApiService.injectDemoSignal(signalData);

            redirectAttributes.addFlashAttribute("message", "데모 신호가 성공적으로 주입되었습니다.");
            return "redirect:/trading/demo-signal";

        } catch (Exception e) {
            log.error("Failed to inject demo signal", e);
            redirectAttributes.addFlashAttribute("error", "데모 신호 주입에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/demo-signal";
        }
    }
}
