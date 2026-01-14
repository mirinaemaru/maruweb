package com.maru.strategy.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy Controller - External Trading System API Only
 * 모든 전략 관리는 외부 Trading System API(포트 8099)를 통해 수행됩니다.
 */
@Slf4j
@Controller
@RequestMapping("/trading/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final TradingApiService tradingApiService;

    /**
     * 전략 목록 페이지
     * 외부 Trading System API에서 전략 목록을 조회합니다.
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<?> strategies = Collections.emptyList();

        try {
            Map<String, Object> strategiesData = tradingApiService.getStrategies();
            strategies = (List<?>) strategiesData.get("items");

            if (strategies == null) {
                strategies = Collections.emptyList();
            }

            // 상태 필터 적용
            if (status != null && !status.isEmpty()) {
                final String filterStatus = status;
                strategies = strategies.stream()
                        .filter(s -> {
                            if (s instanceof Map) {
                                Object strategyStatus = ((Map<?, ?>) s).get("status");
                                return filterStatus.equals(strategyStatus);
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
            }

            // 키워드 필터 적용
            if (keyword != null && !keyword.isEmpty()) {
                final String filterKeyword = keyword.toLowerCase();
                strategies = strategies.stream()
                        .filter(s -> {
                            if (s instanceof Map) {
                                Map<?, ?> strategyMap = (Map<?, ?>) s;
                                String name = (String) strategyMap.get("name");
                                String description = (String) strategyMap.get("description");
                                return (name != null && name.toLowerCase().contains(filterKeyword)) ||
                                       (description != null && description.toLowerCase().contains(filterKeyword));
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Failed to load strategies from Trading System API", e);
            model.addAttribute("error", "Trading System에서 전략 목록을 가져올 수 없습니다: " + e.getMessage());
        }

        model.addAttribute("strategies", strategies);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "strategy/list";
    }

    /**
     * 새 전략 생성 폼
     */
    @GetMapping("/new")
    public String newStrategyForm(Model model) {
        try {
            // 계좌 목록 조회 (전략 생성시 계좌 선택용)
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            model.addAttribute("accounts", accountsData.get("items"));
        } catch (Exception e) {
            log.warn("Failed to load accounts", e);
            model.addAttribute("accounts", Collections.emptyList());
        }

        return "strategy/new";
    }

    /**
     * 새 전략 생성 처리
     */
    @PostMapping("/new")
    public String createStrategy(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String accountId,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Creating new strategy via API: name={}", name);

            Map<String, Object> strategyData = new HashMap<>();
            strategyData.put("name", name);
            if (description != null && !description.isEmpty()) {
                strategyData.put("description", description);
            }
            if (type != null && !type.isEmpty()) {
                strategyData.put("type", type);
            }
            if (symbol != null && !symbol.isEmpty()) {
                strategyData.put("symbol", symbol);
            }
            if (accountId != null && !accountId.isEmpty()) {
                strategyData.put("accountId", accountId);
            }

            Map<String, Object> result = tradingApiService.createStrategy(strategyData);

            if (result != null && result.containsKey("strategyId")) {
                redirectAttributes.addFlashAttribute("success", "전략이 생성되었습니다.");
                return "redirect:/trading/strategies/" + result.get("strategyId");
            } else {
                redirectAttributes.addFlashAttribute("success", "전략이 생성되었습니다.");
                return "redirect:/trading/strategies";
            }

        } catch (Exception e) {
            log.error("Failed to create strategy via API", e);
            redirectAttributes.addFlashAttribute("error", "전략 생성에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/new";
        }
    }

    /**
     * 전략 상세 보기
     */
    @GetMapping("/{id}")
    public String viewStrategy(@PathVariable String id, Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Viewing strategy: {}", id);
            Map<String, Object> strategy = tradingApiService.getStrategy(id);

            if (strategy == null || strategy.containsKey("error")) {
                redirectAttributes.addFlashAttribute("error", "전략을 찾을 수 없습니다.");
                return "redirect:/trading/strategies";
            }

            model.addAttribute("strategy", strategy);

            // 최근 주문 내역 조회
            String accountId = (String) strategy.get("accountId");
            if (accountId != null && !accountId.isEmpty()) {
                try {
                    Map<String, Object> orders = tradingApiService.getOrders(accountId);
                    model.addAttribute("recentOrders", orders.get("items"));
                } catch (Exception e) {
                    log.warn("Failed to load orders for account: {}", accountId, e);
                    model.addAttribute("recentOrders", Collections.emptyList());
                }
            } else {
                model.addAttribute("recentOrders", Collections.emptyList());
            }

            return "strategy/view";

        } catch (Exception e) {
            log.error("Failed to view strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 조회에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies";
        }
    }

    /**
     * 전략 수정 폼
     */
    @GetMapping("/{id}/edit")
    public String editStrategyForm(@PathVariable String id, Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            log.info("Loading edit form for strategy: {}", id);
            Map<String, Object> strategy = tradingApiService.getStrategy(id);

            if (strategy == null || strategy.containsKey("error")) {
                redirectAttributes.addFlashAttribute("error", "전략을 찾을 수 없습니다.");
                return "redirect:/trading/strategies";
            }

            model.addAttribute("strategy", strategy);

            // 계좌 목록 조회
            try {
                Map<String, Object> accountsData = tradingApiService.getAccounts();
                model.addAttribute("accounts", accountsData.get("items"));
            } catch (Exception e) {
                log.warn("Failed to load accounts", e);
                model.addAttribute("accounts", Collections.emptyList());
            }

            return "strategy/edit";

        } catch (Exception e) {
            log.error("Failed to load strategy for editing", e);
            redirectAttributes.addFlashAttribute("error", "전략 조회에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies";
        }
    }

    /**
     * 전략 수정 처리
     */
    @PostMapping("/{id}/edit")
    public String updateStrategy(@PathVariable String id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String symbol,
                                 @RequestParam(required = false) String accountId,
                                 RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating strategy: {}", id);

            Map<String, Object> strategyData = new HashMap<>();
            strategyData.put("name", name);
            if (description != null && !description.isEmpty()) strategyData.put("description", description);
            if (type != null && !type.isEmpty()) strategyData.put("type", type);
            if (status != null && !status.isEmpty()) strategyData.put("status", status);
            if (symbol != null && !symbol.isEmpty()) strategyData.put("symbol", symbol);
            if (accountId != null && !accountId.isEmpty()) strategyData.put("accountId", accountId);

            tradingApiService.updateStrategy(id, strategyData);

            redirectAttributes.addFlashAttribute("success", "전략이 수정되었습니다.");
            return "redirect:/trading/strategies/" + id;

        } catch (Exception e) {
            log.error("Failed to update strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 수정에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/" + id + "/edit";
        }
    }

    /**
     * 자동매매 설정 폼
     */
    @GetMapping("/{id}/trading")
    public String tradingConfigForm(@PathVariable String id, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            log.info("Loading trading config for strategy: {}", id);
            Map<String, Object> strategy = tradingApiService.getStrategy(id);

            if (strategy == null || strategy.containsKey("error")) {
                redirectAttributes.addFlashAttribute("error", "전략을 찾을 수 없습니다.");
                return "redirect:/trading/strategies";
            }

            model.addAttribute("strategy", strategy);

            // 계좌 목록 조회
            try {
                Map<String, Object> accountsData = tradingApiService.getAccounts();
                model.addAttribute("accounts", accountsData.get("items"));
            } catch (Exception e) {
                log.warn("Failed to load accounts", e);
                model.addAttribute("accounts", Collections.emptyList());
            }

            return "strategy/trading-config";

        } catch (Exception e) {
            log.error("Failed to load trading config", e);
            redirectAttributes.addFlashAttribute("error", "전략 조회에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies";
        }
    }

    /**
     * 자동매매 설정 저장
     */
    @PostMapping("/{id}/trading")
    public String updateTradingConfig(@PathVariable String id,
                                      @RequestParam(required = false) String accountId,
                                      @RequestParam(required = false) String assetType,
                                      @RequestParam(required = false) String symbol,
                                      @RequestParam(required = false) String entryConditions,
                                      @RequestParam(required = false) String exitConditions,
                                      @RequestParam(required = false) String stopLossType,
                                      @RequestParam(required = false) Double stopLossValue,
                                      @RequestParam(required = false) String takeProfitType,
                                      @RequestParam(required = false) Double takeProfitValue,
                                      @RequestParam(required = false) String positionSizeType,
                                      @RequestParam(required = false) Double positionSizeValue,
                                      @RequestParam(required = false) Integer maxPositions,
                                      RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating trading config for strategy: {}", id);

            Map<String, Object> configData = new HashMap<>();
            if (accountId != null && !accountId.isEmpty()) configData.put("accountId", accountId);
            if (assetType != null) configData.put("assetType", assetType);
            if (symbol != null) configData.put("symbol", symbol);
            if (entryConditions != null && !entryConditions.isEmpty()) configData.put("entryConditions", entryConditions);
            if (exitConditions != null && !exitConditions.isEmpty()) configData.put("exitConditions", exitConditions);
            if (stopLossType != null) configData.put("stopLossType", stopLossType);
            if (stopLossValue != null) configData.put("stopLossValue", stopLossValue);
            if (takeProfitType != null) configData.put("takeProfitType", takeProfitType);
            if (takeProfitValue != null) configData.put("takeProfitValue", takeProfitValue);
            if (positionSizeType != null) configData.put("positionSizeType", positionSizeType);
            if (positionSizeValue != null) configData.put("positionSizeValue", positionSizeValue);
            if (maxPositions != null) configData.put("maxPositions", maxPositions);

            tradingApiService.updateStrategy(id, configData);

            redirectAttributes.addFlashAttribute("success", "매매 설정이 저장되었습니다.");
            return "redirect:/trading/strategies/" + id;

        } catch (Exception e) {
            log.error("Failed to update trading config", e);
            redirectAttributes.addFlashAttribute("error", "매매 설정 저장에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/" + id + "/trading";
        }
    }

    /**
     * 전략 활성화
     */
    @PostMapping("/{id}/activate")
    public String activateStrategy(@PathVariable String id,
                                   RedirectAttributes redirectAttributes) {
        try {
            log.info("Activating strategy: {}", id);
            tradingApiService.updateStrategyStatus(id, "ACTIVE");
            redirectAttributes.addFlashAttribute("success", "전략이 활성화되었습니다.");
        } catch (Exception e) {
            log.error("Failed to activate strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 활성화에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    /**
     * 전략 비활성화
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateStrategy(@PathVariable String id,
                                     RedirectAttributes redirectAttributes) {
        try {
            log.info("Deactivating strategy: {}", id);
            tradingApiService.updateStrategyStatus(id, "INACTIVE");
            redirectAttributes.addFlashAttribute("success", "전략이 비활성화되었습니다.");
        } catch (Exception e) {
            log.error("Failed to deactivate strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 비활성화에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    /**
     * 전략 삭제
     */
    @PostMapping("/{id}/delete")
    public String deleteStrategy(@PathVariable String id,
                                 RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting strategy: {}", id);
            tradingApiService.deleteStrategy(id);
            redirectAttributes.addFlashAttribute("success", "전략이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Failed to delete strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 삭제에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }
}
