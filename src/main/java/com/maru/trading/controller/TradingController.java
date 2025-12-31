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
            @RequestParam String broker,
            @RequestParam String cano,
            @RequestParam String acntPrdtCd,
            @RequestParam String alias,
            @RequestParam String environment,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new account: broker={}, cano={}, alias={}", broker, cano, alias);

            Map<String, Object> accountData = new HashMap<>();
            accountData.put("broker", broker);
            accountData.put("environment", environment);
            accountData.put("cano", cano);
            accountData.put("acntPrdtCd", acntPrdtCd);
            accountData.put("alias", alias);

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
     * 계좌 상세보기 페이지
     */
    @GetMapping("/accounts/{accountId}")
    public String viewAccount(@PathVariable String accountId, Model model) {
        try {
            log.info("Loading Account Detail page - accountId: {}", accountId);

            Map<String, Object> account = tradingApiService.getAccount(accountId);
            model.addAttribute("account", account);

            return "trading/account-detail";

        } catch (Exception e) {
            log.error("Failed to load account detail page", e);
            model.addAttribute("error", "계좌 상세 정보를 불러올 수 없습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/error";
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
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating account: accountId={}, alias={}, environment={}", accountId, alias, environment);

            Map<String, Object> accountData = new HashMap<>();
            accountData.put("alias", alias);
            accountData.put("environment", environment);

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

            // accountId가 없으면 첫 번째 계좌 자동 선택
            if ((accountId == null || accountId.isEmpty()) && accounts != null && !accounts.isEmpty()) {
                Map<String, Object> firstAccount = (Map<String, Object>) accounts.get(0);
                accountId = (String) firstAccount.get("accountId");
                log.info("Auto-selecting first account: {}", accountId);
            }

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
     * 포지션 조회 페이지
     */
    @GetMapping("/positions")
    public String positions(@RequestParam(required = false) String accountId, Model model) {
        try {
            log.info("Loading Trading Positions page - accountId: {}", accountId);

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

            // 포지션 목록 및 계좌 잔액
            if (accountId != null && !accountId.isEmpty()) {
                // 포지션 목록 조회
                Map<String, Object> positionsData = tradingApiService.getPositions(accountId);
                List<?> positions = (List<?>) positionsData.get("items");
                Integer total = (Integer) positionsData.get("total");
                model.addAttribute("positions", positions);
                model.addAttribute("totalPositions", total != null ? total : 0);

                // 계좌 잔액 조회
                try {
                    Map<String, Object> balanceData = tradingApiService.getAccountBalance(accountId);
                    model.addAttribute("balance", balanceData);
                } catch (Exception e) {
                    log.warn("Failed to fetch account balance for accountId: {}", accountId, e);
                    model.addAttribute("balanceError", "계좌 잔액을 조회할 수 없습니다.");
                }
            }

            model.addAttribute("apiConnected", true);
            return "trading/positions";

        } catch (Exception e) {
            log.error("Failed to load positions", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/positions";
        }
    }

    /**
     * Kill Switch 관리 페이지
     */
    @GetMapping("/kill-switch")
    public String killSwitch(Model model) {
        try {
            log.info("Loading Kill Switch Management page");

            // Kill Switch 상태 조회
            Map<String, Object> killSwitch = tradingApiService.getKillSwitchStatus();
            model.addAttribute("killSwitch", killSwitch);
            model.addAttribute("killSwitchStatus", killSwitch.get("status"));

            // 계좌 목록 (계좌별 Kill Switch 설정용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            List<?> accounts = (List<?>) accountsData.get("items");
            model.addAttribute("accounts", accounts);

            model.addAttribute("apiConnected", true);
            return "trading/kill-switch";

        } catch (Exception e) {
            log.error("Failed to load Kill Switch page", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/kill-switch";
        }
    }

    /**
     * Kill Switch 토글 처리
     */
    @PostMapping("/kill-switch/toggle")
    public String toggleKillSwitch(
            @RequestParam String status,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String accountId,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Toggling Kill Switch: status={}, reason={}, accountId={}", status, reason, accountId);

            tradingApiService.toggleKillSwitch(status, reason, accountId);

            String scope = (accountId != null && !accountId.isEmpty()) ? "계좌별" : "전역";
            String message = String.format("Kill Switch가 %s로 설정되었습니다. (%s)", status, scope);
            redirectAttributes.addFlashAttribute("message", message);

            return "redirect:/trading/kill-switch";

        } catch (Exception e) {
            log.error("Failed to toggle Kill Switch", e);
            redirectAttributes.addFlashAttribute("error", "Kill Switch 설정에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/kill-switch";
        }
    }

    /**
     * 체결 내역 조회 페이지
     */
    @GetMapping("/fills")
    public String fills(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String symbol,
            Model model) {
        try {
            log.info("Loading Trading Fills page - accountId: {}, orderId: {}, symbol: {}", accountId, orderId, symbol);

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
            model.addAttribute("selectedOrderId", orderId);
            model.addAttribute("selectedSymbol", symbol);

            // 체결 내역 조회
            if (accountId != null && !accountId.isEmpty()) {
                Map<String, Object> fillsData = tradingApiService.getFills(accountId, orderId, symbol);
                List<?> fills = (List<?>) fillsData.get("items");
                model.addAttribute("fills", fills);
                model.addAttribute("fillCount", fills != null ? fills.size() : 0);
            }

            model.addAttribute("apiConnected", true);
            return "trading/fills";

        } catch (Exception e) {
            log.error("Failed to load fills", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/fills";
        }
    }

    /**
     * 잔고 조회 페이지
     */
    @GetMapping("/balances")
    public String balances(@RequestParam(required = false) String accountId, Model model) {
        try {
            log.info("Loading Trading Balances page - accountId: {}", accountId);

            // 계좌 목록 (선택용)
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

            // 잔고 조회
            if (accountId != null && !accountId.isEmpty()) {
                Map<String, Object> balanceData = tradingApiService.getBalance(accountId);
                model.addAttribute("balance", balanceData);
            }

            model.addAttribute("apiConnected", true);
            return "trading/balances";

        } catch (Exception e) {
            log.error("Failed to load balances", e);
            model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
            model.addAttribute("errorDetail", e.getMessage());
            model.addAttribute("apiConnected", false);
            return "trading/balances";
        }
    }

    /**
     * 주문 취소 처리
     */
    @PostMapping("/orders/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable String orderId,
            @RequestParam String accountId,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Cancelling order: orderId={}", orderId);

            Map<String, Object> result = tradingApiService.cancelOrder(orderId);

            redirectAttributes.addFlashAttribute("message", "주문이 성공적으로 취소되었습니다.");
            return "redirect:/trading/orders?accountId=" + accountId;

        } catch (Exception e) {
            log.error("Failed to cancel order: {}", orderId, e);
            redirectAttributes.addFlashAttribute("error", "주문 취소에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/orders?accountId=" + accountId;
        }
    }

    /**
     * 주문 수정 처리
     */
    @PostMapping("/orders/{orderId}/modify")
    public String modifyOrder(
            @PathVariable String orderId,
            @RequestParam String accountId,
            @RequestParam(required = false) Double newPrice,
            @RequestParam(required = false) Integer newQuantity,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Modifying order: orderId={}, newPrice={}, newQuantity={}", orderId, newPrice, newQuantity);

            if (newPrice == null && newQuantity == null) {
                redirectAttributes.addFlashAttribute("error", "수정할 가격 또는 수량을 입력하세요.");
                return "redirect:/trading/orders?accountId=" + accountId;
            }

            Map<String, Object> result = tradingApiService.modifyOrder(orderId, newPrice, newQuantity);

            redirectAttributes.addFlashAttribute("message", "주문이 성공적으로 수정되었습니다.");
            return "redirect:/trading/orders?accountId=" + accountId;

        } catch (Exception e) {
            log.error("Failed to modify order: {}", orderId, e);
            redirectAttributes.addFlashAttribute("error", "주문 수정에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/orders?accountId=" + accountId;
        }
    }

    /**
     * 계좌 상태 업데이트 처리
     */
    @PostMapping("/accounts/{accountId}/status")
    public String updateAccountStatus(
            @PathVariable String accountId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating account status: accountId={}, status={}", accountId, status);

            Map<String, Object> result = tradingApiService.updateAccountStatus(accountId, status);

            String message = "ACTIVE".equals(status) ? "계좌가 활성화되었습니다." : "계좌가 비활성화되었습니다.";
            redirectAttributes.addFlashAttribute("message", message);
            return "redirect:/trading/accounts";

        } catch (Exception e) {
            log.error("Failed to update account status: {}", accountId, e);
            redirectAttributes.addFlashAttribute("error", "계좌 상태 업데이트에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/accounts";
        }
    }

}
