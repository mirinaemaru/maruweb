package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

            return "trading/accounts";

        } catch (Exception e) {
            log.error("Failed to load accounts", e);
            model.addAttribute("error", "계좌 목록을 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
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

            return "trading/strategies";

        } catch (Exception e) {
            log.error("Failed to load strategies", e);
            model.addAttribute("error", "전략 목록을 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
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

            return "trading/orders";

        } catch (Exception e) {
            log.error("Failed to load orders", e);
            model.addAttribute("error", "주문 목록을 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
        }
    }
}
