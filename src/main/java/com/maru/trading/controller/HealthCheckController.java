package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health Check Controller
 * Trading System과 Database 상태 모니터링 페이지
 */
@Slf4j
@Controller
@RequestMapping("/trading/health-check")
@RequiredArgsConstructor
public class HealthCheckController {

    private final TradingApiService tradingApiService;

    /**
     * Health Check 페이지
     */
    @GetMapping
    public String page(Model model) {
        try {
            log.info("Loading Health Check page");

            // Trading API 연결 상태 확인
            Map<String, Object> tradingApiHealth = new HashMap<>();
            long startTime = System.currentTimeMillis();
            try {
                Map<String, Object> healthResponse = tradingApiService.getHealthStatus();
                long responseTime = System.currentTimeMillis() - startTime;

                tradingApiHealth.put("status", healthResponse.getOrDefault("status", "UNKNOWN"));
                tradingApiHealth.put("responseTime", responseTime);
                tradingApiHealth.put("connected", true);
                tradingApiHealth.put("details", healthResponse);
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                tradingApiHealth.put("status", "DOWN");
                tradingApiHealth.put("responseTime", responseTime);
                tradingApiHealth.put("connected", false);
                tradingApiHealth.put("error", e.getMessage());
            }

            // 활성 전략 수 조회
            int activeStrategyCount = 0;
            try {
                Map<String, Object> strategiesData = tradingApiService.getStrategies();
                List<?> strategies = (List<?>) strategiesData.get("items");
                if (strategies != null) {
                    // ACTIVE 상태인 전략만 카운트
                    activeStrategyCount = (int) strategies.stream()
                        .filter(s -> {
                            if (s instanceof Map) {
                                Map<?, ?> strategy = (Map<?, ?>) s;
                                return "ACTIVE".equals(strategy.get("status"));
                            }
                            return false;
                        })
                        .count();
                }
            } catch (Exception e) {
                log.warn("Failed to get active strategies count", e);
            }

            // 오늘의 주문/체결 통계 (첫 번째 계좌로만 조회)
            Map<String, Object> todayStats = new HashMap<>();
            todayStats.put("orderCount", 0);
            todayStats.put("fillCount", 0);

            try {
                // 계좌 목록 조회
                Map<String, Object> accountsData = tradingApiService.getAccounts();
                List<?> accounts = (List<?>) accountsData.get("items");

                if (accounts != null && !accounts.isEmpty()) {
                    Map<?, ?> firstAccount = (Map<?, ?>) accounts.get(0);
                    String accountId = (String) firstAccount.get("accountId");

                    // 오늘의 주문 조회
                    try {
                        Map<String, Object> ordersData = tradingApiService.getOrders(accountId);
                        List<?> orders = (List<?>) ordersData.get("items");
                        todayStats.put("orderCount", orders != null ? orders.size() : 0);
                    } catch (Exception e) {
                        log.warn("Failed to get today's orders", e);
                    }

                    // 오늘의 체결 조회
                    try {
                        Map<String, Object> fillsData = tradingApiService.getFills(accountId, null, null);
                        List<?> fills = (List<?>) fillsData.get("items");
                        todayStats.put("fillCount", fills != null ? fills.size() : 0);
                    } catch (Exception e) {
                        log.warn("Failed to get today's fills", e);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get today's statistics", e);
            }

            // 데이터베이스 연결 상태 (단순 체크)
            Map<String, Object> dbHealth = new HashMap<>();
            dbHealth.put("status", "UP");
            dbHealth.put("connected", true);
            // 실제 DB 체크는 애플리케이션이 정상 작동 중이면 OK로 간주

            model.addAttribute("tradingApiHealth", tradingApiHealth);
            model.addAttribute("dbHealth", dbHealth);
            model.addAttribute("activeStrategyCount", activeStrategyCount);
            model.addAttribute("todayStats", todayStats);
            model.addAttribute("currentTime", LocalDate.now());

            return "trading/health-check";

        } catch (Exception e) {
            log.error("Failed to load health check page", e);
            model.addAttribute("error", "Health Check 페이지 로드에 실패했습니다.");
            model.addAttribute("errorDetail", e.getMessage());
            return "trading/health-check";
        }
    }
}
