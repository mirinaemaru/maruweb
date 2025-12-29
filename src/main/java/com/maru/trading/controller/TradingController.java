package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trading System Controller
 * Trading System Dashboard 및 관리 기능 제공
 */
@Slf4j
@Controller
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {

    private final TradingApiService tradingApiService;

    /**
     * Trading Dashboard - 메인 화면
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            log.info("Loading Trading Dashboard");

            // 1. Health Check
            Map<String, Object> health = tradingApiService.getHealthStatus();
            model.addAttribute("health", health);
            model.addAttribute("systemStatus", health.get("status"));

            // 2. Kill Switch Status
            Map<String, Object> killSwitch = tradingApiService.getKillSwitchStatus();
            model.addAttribute("killSwitch", killSwitch);
            model.addAttribute("killSwitchStatus", killSwitch.get("status"));

            // 3. Accounts
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);
            model.addAttribute("accountCount", accounts != null ? accounts.size() : 0);

            // 4. Strategies
            Map<String, Object> strategiesData = tradingApiService.getStrategies();
            List<?> strategies = (List<?>) strategiesData.get("items");
            model.addAttribute("strategies", strategies);
            model.addAttribute("strategyCount", strategies != null ? strategies.size() : 0);

            // Active strategies count
            long activeStrategyCount = 0;
            if (strategies != null) {
                activeStrategyCount = strategies.stream()
                        .filter(s -> "ACTIVE".equals(((Map<?, ?>) s).get("status")))
                        .count();
            }
            model.addAttribute("activeStrategyCount", activeStrategyCount);

            return "trading/dashboard";

        } catch (Exception e) {
            log.error("Failed to load Trading Dashboard", e);
            model.addAttribute("error", "Trading System에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 계좌 관리 페이지
     */
    @GetMapping("/accounts")
    public String accounts(Model model) {
        try {
            log.info("Loading Trading Accounts page");

            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);
            model.addAttribute("apiConnected", true);

            return "trading/accounts";

        } catch (Exception e) {
            log.error("Failed to load accounts", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/accounts";
        }
    }

    /**
     * 계좌 등록 페이지
     */
    @GetMapping("/accounts/new")
    public String newAccount(Model model) {
        try {
            log.info("Loading Account Registration page");
            return "trading/account-form";

        } catch (Exception e) {
            log.error("Failed to load account registration page", e);
            model.addAttribute("error", "계좌 등록 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 계좌 등록 처리
     */
    @PostMapping("/accounts")
    public String createAccount(
            @RequestParam String accountId,
            @RequestParam String alias,
            @RequestParam String environment,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new account: {}", alias);

            Map<String, Object> accountData = new HashMap<>();
            accountData.put("accountId", accountId);
            accountData.put("alias", alias);
            accountData.put("environment", environment);
            accountData.put("description", description);
            accountData.put("status", "ACTIVE");

            tradingApiService.createAccount(accountData);

            redirectAttributes.addFlashAttribute("message", "계좌가 성공적으로 등록되었습니다.");
            return "redirect:/trading/accounts";

        } catch (Exception e) {
            log.error("Failed to create account", e);
            redirectAttributes.addFlashAttribute("error", "계좌 등록에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/accounts/new";
        }
    }

    /**
     * 계좌 수정 페이지
     */
    @GetMapping("/accounts/{accountId}/edit")
    public String editAccount(@PathVariable String accountId, Model model) {
        try {
            log.info("Loading Account Edit page - accountId: {}", accountId);

            Map<String, Object> account = tradingApiService.getAccount(accountId);
            model.addAttribute("account", account);

            return "trading/account-form";

        } catch (Exception e) {
            log.error("Failed to load account edit page", e);
            model.addAttribute("error", "계좌 수정 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 계좌 수정 처리
     */
    @PostMapping("/accounts/{accountId}")
    public String updateAccount(
            @PathVariable String accountId,
            @RequestParam String alias,
            @RequestParam String environment,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating account: {}", accountId);

            Map<String, Object> accountData = new HashMap<>();
            accountData.put("alias", alias);
            accountData.put("environment", environment);
            accountData.put("description", description);

            tradingApiService.updateAccount(accountId, accountData);

            redirectAttributes.addFlashAttribute("message", "계좌가 성공적으로 수정되었습니다.");
            return "redirect:/trading/accounts";

        } catch (Exception e) {
            log.error("Failed to update account", e);
            redirectAttributes.addFlashAttribute("error", "계좌 수정에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/accounts/" + accountId + "/edit";
        }
    }

    /**
     * 계좌 삭제
     */
    @PostMapping("/accounts/{accountId}/delete")
    public String deleteAccount(@PathVariable String accountId, RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting account: {}", accountId);

            tradingApiService.deleteAccount(accountId);

            redirectAttributes.addFlashAttribute("message", "계좌가 성공적으로 삭제되었습니다.");
            return "redirect:/trading/accounts";

        } catch (Exception e) {
            log.error("Failed to delete account", e);
            redirectAttributes.addFlashAttribute("error", "계좌 삭제에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/accounts";
        }
    }

    /**
     * 전략 관리 페이지
     */
    @GetMapping("/strategies")
    public String strategies(Model model) {
        try {
            log.info("Loading Trading Strategies page");

            Map<String, Object> strategiesData = tradingApiService.getStrategies();
            List<?> strategies = (List<?>) strategiesData.get("items");
            model.addAttribute("strategies", strategies);
            model.addAttribute("apiConnected", true);

            return "trading/strategies";

        } catch (Exception e) {
            log.error("Failed to load strategies", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/strategies";
        }
    }

    /**
     * 주문 조회 페이지
     */
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String accountId, Model model) {
        try {
            log.info("Loading Trading Orders page - accountId: {}", accountId);

            // 계좌 목록 (필터용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);
            model.addAttribute("selectedAccountId", accountId);

            // 주문 목록
            if (accountId != null && !accountId.isEmpty()) {
                Map<String, Object> ordersData = tradingApiService.getOrders(accountId);
                List<?> orders = (List<?>) ordersData.get("items");
                model.addAttribute("orders", orders);
            }

            model.addAttribute("apiConnected", true);
            return "trading/orders";

        } catch (Exception e) {
            log.error("Failed to load orders", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/orders";
        }
    }

    /**
     * 전략 등록 페이지
     */
    @GetMapping("/strategies/new")
    public String newStrategy(Model model) {
        try {
            log.info("Loading Strategy Registration page");

            // 계좌 목록 (전략 등록 시 선택용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);

            return "trading/strategy-form";

        } catch (Exception e) {
            log.error("Failed to load strategy registration page", e);
            model.addAttribute("error", "전략 등록 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 전략 등록 처리
     */
    @PostMapping("/strategies")
    public String createStrategy(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam String accountId,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new strategy: {}", name);

            Map<String, Object> strategyData = new HashMap<>();
            strategyData.put("name", name);
            strategyData.put("type", type);
            strategyData.put("accountId", accountId);
            strategyData.put("description", description);
            strategyData.put("status", "INACTIVE");

            tradingApiService.createStrategy(strategyData);

            redirectAttributes.addFlashAttribute("message", "전략이 성공적으로 등록되었습니다.");
            return "redirect:/trading/strategies";

        } catch (Exception e) {
            log.error("Failed to create strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 등록에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/new";
        }
    }

    /**
     * 전략 수정 페이지
     */
    @GetMapping("/strategies/{strategyId}/edit")
    public String editStrategy(@PathVariable String strategyId, Model model) {
        try {
            log.info("Loading Strategy Edit page - strategyId: {}", strategyId);

            // 전략 상세 정보
            Map<String, Object> strategy = tradingApiService.getStrategy(strategyId);
            model.addAttribute("strategy", strategy);

            // 계좌 목록
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);

            return "trading/strategy-form";

        } catch (Exception e) {
            log.error("Failed to load strategy edit page", e);
            model.addAttribute("error", "전략 수정 페이지를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 전략 수정 처리
     */
    @PostMapping("/strategies/{strategyId}")
    public String updateStrategy(
            @PathVariable String strategyId,
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam String accountId,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating strategy: {}", strategyId);

            Map<String, Object> strategyData = new HashMap<>();
            strategyData.put("name", name);
            strategyData.put("type", type);
            strategyData.put("accountId", accountId);
            strategyData.put("description", description);

            tradingApiService.updateStrategy(strategyId, strategyData);

            redirectAttributes.addFlashAttribute("message", "전략이 성공적으로 수정되었습니다.");
            return "redirect:/trading/strategies";

        } catch (Exception e) {
            log.error("Failed to update strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 수정에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/" + strategyId + "/edit";
        }
    }

    /**
     * 전략 삭제
     */
    @PostMapping("/strategies/{strategyId}/delete")
    public String deleteStrategy(@PathVariable String strategyId, RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting strategy: {}", strategyId);

            tradingApiService.deleteStrategy(strategyId);

            redirectAttributes.addFlashAttribute("message", "전략이 성공적으로 삭제되었습니다.");
            return "redirect:/trading/strategies";

        } catch (Exception e) {
            log.error("Failed to delete strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 삭제에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies";
        }
    }

    /**
     * 전략 상태 변경
     */
    @PostMapping("/strategies/{strategyId}/status")
    public String updateStrategyStatus(
            @PathVariable String strategyId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating strategy status: {} -> {}", strategyId, status);

            tradingApiService.updateStrategyStatus(strategyId, status);

            redirectAttributes.addFlashAttribute("message", "전략 상태가 변경되었습니다.");
            return "redirect:/trading/strategies";

        } catch (Exception e) {
            log.error("Failed to update strategy status", e);
            redirectAttributes.addFlashAttribute("error", "전략 상태 변경에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies";
        }
    }
}
