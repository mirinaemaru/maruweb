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
 * Risk Rule Management Controller
 * 리스크 룰 관리 (전역, 계좌별, 종목별)
 */
@Slf4j
@Controller
@RequestMapping("/trading/risk-rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final TradingApiService tradingApiService;

    /**
     * 리스크 룰 목록 페이지
     */
    @GetMapping
    public String list(@RequestParam(required = false) String accountId, Model model) {
        try {
            log.info("Loading Risk Rules page - accountId: {}", accountId);

            // 계좌 목록 (필터용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);

            // accountId가 없으면 첫 번째 계좌 자동 선택
            if ((accountId == null || accountId.isEmpty()) && accounts != null && !accounts.isEmpty()) {
                Map<String, Object> firstAccount = (Map<String, Object>) accounts.get(0);
                accountId = (String) firstAccount.get("accountId");
                log.info("Auto-selecting first account: {}", accountId);
            }

            model.addAttribute("selectedAccountId", accountId);

            // 리스크 룰 목록 조회
            if (accountId != null && !accountId.isEmpty()) {
                Map<String, Object> rulesData = tradingApiService.getRiskRulesForAccount(accountId);
                List<?> rules = (List<?>) rulesData.get("rules");
                model.addAttribute("rules", rules);
                model.addAttribute("ruleCount", rules != null ? rules.size() : 0);
            }

            model.addAttribute("apiConnected", true);
            return "trading/risk-rules/list";

        } catch (Exception e) {
            log.error("Failed to load risk rules", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/risk-rules/list";
        }
    }

    /**
     * 전역 리스크 룰 생성/수정 페이지
     */
    @GetMapping("/global/edit")
    public String editGlobal(Model model) {
        try {
            log.info("Loading Global Risk Rule Edit page");
            model.addAttribute("scope", "GLOBAL");
            model.addAttribute("pageTitle", "전역 리스크 룰 설정");
            return "trading/risk-rules/edit";

        } catch (Exception e) {
            log.error("Failed to load global risk rule edit page", e);
            model.addAttribute("error", "리스크 룰 수정 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 계좌별 리스크 룰 생성/수정 페이지
     */
    @GetMapping("/account/{accountId}/edit")
    public String editAccount(@PathVariable String accountId, Model model) {
        try {
            log.info("Loading Account Risk Rule Edit page - accountId: {}", accountId);

            // 계좌 정보 조회
            Map<String, Object> account = tradingApiService.getAccount(accountId);
            model.addAttribute("account", account);
            model.addAttribute("scope", "ACCOUNT");
            model.addAttribute("pageTitle", "계좌별 리스크 룰 설정");

            return "trading/risk-rules/edit";

        } catch (Exception e) {
            log.error("Failed to load account risk rule edit page", e);
            model.addAttribute("error", "리스크 룰 수정 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 종목별 리스크 룰 생성/수정 페이지
     */
    @GetMapping("/account/{accountId}/symbol/edit")
    public String editSymbol(@PathVariable String accountId,
                            @RequestParam(required = false) String symbol,
                            Model model) {
        try {
            log.info("Loading Symbol Risk Rule Edit page - accountId: {}, symbol: {}", accountId, symbol);

            // 계좌 정보 조회
            Map<String, Object> account = tradingApiService.getAccount(accountId);
            model.addAttribute("account", account);
            model.addAttribute("symbol", symbol);
            model.addAttribute("scope", "SYMBOL");
            model.addAttribute("pageTitle", "종목별 리스크 룰 설정");

            return "trading/risk-rules/edit";

        } catch (Exception e) {
            log.error("Failed to load symbol risk rule edit page", e);
            model.addAttribute("error", "리스크 룰 수정 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 전역 리스크 룰 저장
     */
    @PostMapping("/global")
    public String saveGlobal(
            @RequestParam(required = false) BigDecimal maxPositionValuePerSymbol,
            @RequestParam(required = false) Integer maxOpenOrders,
            @RequestParam(required = false) Integer maxOrdersPerMinute,
            @RequestParam(required = false) BigDecimal dailyLossLimit,
            @RequestParam(required = false) Integer consecutiveOrderFailuresLimit,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving global risk rule");

            Map<String, Object> ruleData = new HashMap<>();
            if (maxPositionValuePerSymbol != null) {
                ruleData.put("maxPositionValuePerSymbol", maxPositionValuePerSymbol);
            }
            if (maxOpenOrders != null) {
                ruleData.put("maxOpenOrders", maxOpenOrders);
            }
            if (maxOrdersPerMinute != null) {
                ruleData.put("maxOrdersPerMinute", maxOrdersPerMinute);
            }
            if (dailyLossLimit != null) {
                ruleData.put("dailyLossLimit", dailyLossLimit);
            }
            if (consecutiveOrderFailuresLimit != null) {
                ruleData.put("consecutiveOrderFailuresLimit", consecutiveOrderFailuresLimit);
            }

            tradingApiService.updateGlobalRiskRule(ruleData);

            redirectAttributes.addFlashAttribute("message", "전역 리스크 룰이 성공적으로 저장되었습니다.");
            return "redirect:/trading/risk-rules";

        } catch (Exception e) {
            log.error("Failed to save global risk rule", e);
            redirectAttributes.addFlashAttribute("error", "전역 리스크 룰 저장에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/risk-rules/global/edit";
        }
    }

    /**
     * 계좌별 리스크 룰 저장
     */
    @PostMapping("/account/{accountId}")
    public String saveAccount(
            @PathVariable String accountId,
            @RequestParam(required = false) BigDecimal maxPositionValuePerSymbol,
            @RequestParam(required = false) Integer maxOpenOrders,
            @RequestParam(required = false) Integer maxOrdersPerMinute,
            @RequestParam(required = false) BigDecimal dailyLossLimit,
            @RequestParam(required = false) Integer consecutiveOrderFailuresLimit,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving account risk rule for accountId: {}", accountId);

            Map<String, Object> ruleData = new HashMap<>();
            if (maxPositionValuePerSymbol != null) {
                ruleData.put("maxPositionValuePerSymbol", maxPositionValuePerSymbol);
            }
            if (maxOpenOrders != null) {
                ruleData.put("maxOpenOrders", maxOpenOrders);
            }
            if (maxOrdersPerMinute != null) {
                ruleData.put("maxOrdersPerMinute", maxOrdersPerMinute);
            }
            if (dailyLossLimit != null) {
                ruleData.put("dailyLossLimit", dailyLossLimit);
            }
            if (consecutiveOrderFailuresLimit != null) {
                ruleData.put("consecutiveOrderFailuresLimit", consecutiveOrderFailuresLimit);
            }

            tradingApiService.updateAccountRiskRule(accountId, ruleData);

            redirectAttributes.addFlashAttribute("message", "계좌별 리스크 룰이 성공적으로 저장되었습니다.");
            return "redirect:/trading/risk-rules?accountId=" + accountId;

        } catch (Exception e) {
            log.error("Failed to save account risk rule", e);
            redirectAttributes.addFlashAttribute("error", "계좌 리스크 룰 저장에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/risk-rules/account/" + accountId + "/edit";
        }
    }

    /**
     * 종목별 리스크 룰 저장
     */
    @PostMapping("/account/{accountId}/symbol")
    public String saveSymbol(
            @PathVariable String accountId,
            @RequestParam String symbol,
            @RequestParam(required = false) BigDecimal maxPositionValuePerSymbol,
            @RequestParam(required = false) Integer maxOpenOrders,
            @RequestParam(required = false) Integer maxOrdersPerMinute,
            @RequestParam(required = false) BigDecimal dailyLossLimit,
            @RequestParam(required = false) Integer consecutiveOrderFailuresLimit,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving symbol risk rule for accountId: {}, symbol: {}", accountId, symbol);

            Map<String, Object> ruleData = new HashMap<>();
            if (maxPositionValuePerSymbol != null) {
                ruleData.put("maxPositionValuePerSymbol", maxPositionValuePerSymbol);
            }
            if (maxOpenOrders != null) {
                ruleData.put("maxOpenOrders", maxOpenOrders);
            }
            if (maxOrdersPerMinute != null) {
                ruleData.put("maxOrdersPerMinute", maxOrdersPerMinute);
            }
            if (dailyLossLimit != null) {
                ruleData.put("dailyLossLimit", dailyLossLimit);
            }
            if (consecutiveOrderFailuresLimit != null) {
                ruleData.put("consecutiveOrderFailuresLimit", consecutiveOrderFailuresLimit);
            }

            tradingApiService.updateSymbolRiskRule(accountId, symbol, ruleData);

            redirectAttributes.addFlashAttribute("message", "종목별 리스크 룰이 성공적으로 저장되었습니다.");
            return "redirect:/trading/risk-rules?accountId=" + accountId;

        } catch (Exception e) {
            log.error("Failed to save symbol risk rule", e);
            redirectAttributes.addFlashAttribute("error", "종목 리스크 룰 저장에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/risk-rules/account/" + accountId + "/symbol/edit?symbol=" + symbol;
        }
    }

    /**
     * 리스크 룰 삭제
     */
    @PostMapping("/{ruleId}/delete")
    public String delete(
            @PathVariable String ruleId,
            @RequestParam String accountId,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting risk rule: {}", ruleId);

            tradingApiService.deleteRiskRule(ruleId);

            redirectAttributes.addFlashAttribute("message", "리스크 룰이 성공적으로 삭제되었습니다.");
            return "redirect:/trading/risk-rules?accountId=" + accountId;

        } catch (Exception e) {
            log.error("Failed to delete risk rule", e);
            redirectAttributes.addFlashAttribute("error", "리스크 룰 삭제에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/risk-rules?accountId=" + accountId;
        }
    }
}
