package com.maru.strategy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.strategy.dto.TradingStrategyDto;
import com.maru.strategy.entity.Strategy;
import com.maru.strategy.entity.StrategyCategory;
import com.maru.strategy.repository.StrategyRepository;
import com.maru.strategy.service.StrategyService;
import com.maru.strategy.service.TradingStrategyService;
import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/trading/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;
    private final TradingStrategyService tradingStrategyService;
    private final TradingApiService tradingApiService;
    private final StrategyRepository strategyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Display strategies list page with filtering
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetTo,
            Model model) {

        List<Strategy> strategies;

        // Apply filters
        if (keyword != null || status != null || priority != null || categoryId != null) {
            strategies = strategyService.searchStrategies(status, priority, categoryId, keyword);
        } else if (startFrom != null && startTo != null) {
            strategies = strategyService.getStrategiesByStartDateRange(startFrom, startTo);
        } else if (targetFrom != null && targetTo != null) {
            strategies = strategyService.getStrategiesByTargetDateRange(targetFrom, targetTo);
        } else {
            strategies = strategyService.getAllStrategies();
        }

        List<StrategyCategory> categories = strategyService.getAllCategories();

        model.addAttribute("strategies", strategies);
        model.addAttribute("categories", categories);
        model.addAttribute("newStrategy", new Strategy());
        model.addAttribute("newCategory", new StrategyCategory());

        // Filter parameters
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startFrom", startFrom);
        model.addAttribute("startTo", startTo);
        model.addAttribute("targetFrom", targetFrom);
        model.addAttribute("targetTo", targetTo);

        return "strategy/list";
    }

    /**
     * Display strategy edit form
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Strategy strategy = strategyService.getStrategyById(id)
                .orElse(null);
        if (strategy == null) {
            redirectAttributes.addFlashAttribute("error", "Strategy not found");
            return "redirect:/trading/strategies";
        }

        List<StrategyCategory> categories = strategyService.getAllCategories();

        model.addAttribute("strategy", strategy);
        model.addAttribute("categories", categories);
        return "strategy/edit";
    }

    // ========== Category CRUD ==========

    @PostMapping("/categories")
    public String createCategory(
            @Valid @ModelAttribute("newCategory") StrategyCategory category,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required");
            return "redirect:/trading/strategies";
        }

        strategyService.createCategory(category);
        redirectAttributes.addFlashAttribute("success", "Category created successfully");
        return "redirect:/trading/strategies";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @ModelAttribute StrategyCategory category,
            RedirectAttributes redirectAttributes) {

        try {
            strategyService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("success", "Category updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update category: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            strategyService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete category: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    // ========== Strategy CRUD ==========

    @PostMapping
    public String createStrategy(
            @Valid @ModelAttribute("newStrategy") Strategy strategy,
            @RequestParam(required = false) Long categoryId,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title is required");
            return "redirect:/trading/strategies";
        }

        if (categoryId != null) {
            StrategyCategory category = strategyService.getCategoryById(categoryId)
                    .orElse(null);
            if (category == null) {
                redirectAttributes.addFlashAttribute("error", "Category not found");
                return "redirect:/trading/strategies";
            }
            strategy.setCategory(category);
        }

        strategyService.createStrategy(strategy);
        redirectAttributes.addFlashAttribute("success", "Strategy created successfully");
        return "redirect:/trading/strategies";
    }

    @PostMapping("/{id}")
    public String updateStrategy(
            @PathVariable Long id,
            @ModelAttribute Strategy strategy,
            @RequestParam(required = false) Long categoryId,
            RedirectAttributes redirectAttributes) {

        try {
            if (categoryId != null && categoryId > 0) {
                StrategyCategory category = strategyService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));
                strategy.setCategory(category);
            } else {
                strategy.setCategory(null);
            }

            strategyService.updateStrategy(id, strategy);
            redirectAttributes.addFlashAttribute("success", "Strategy updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update strategy: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    @PostMapping("/{id}/delete")
    public String deleteStrategy(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            strategyService.deleteStrategy(id);
            redirectAttributes.addFlashAttribute("success", "Strategy deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete strategy: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    // ========== Auto-Trading Endpoints ==========

    /**
     * Show auto-trading configuration page
     */
    @GetMapping("/{id}/trading")
    public String configureTradingStrategy(@PathVariable Long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        Strategy strategy = strategyService.getStrategyById(id)
                .orElse(null);
        if (strategy == null) {
            redirectAttributes.addFlashAttribute("error", "Strategy not found");
            return "redirect:/trading/strategies";
        }

        // Get accounts for selection
        try {
            Map<String, Object> accountsData = tradingApiService.getAccounts();
            model.addAttribute("accounts", accountsData.get("items"));
        } catch (Exception e) {
            log.warn("Failed to load accounts", e);
            model.addAttribute("accounts", Collections.emptyList());
        }

        model.addAttribute("strategy", strategy);
        return "strategy/trading-config";
    }

    /**
     * Update auto-trading configuration
     */
    @PostMapping("/{id}/trading")
    public String updateTradingConfig(@PathVariable Long id,
                                     @RequestParam String targetAccountId,
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
            // Update strategy with trading configuration
            Strategy strategy = strategyService.getStrategyById(id)
                    .orElseThrow(() -> new RuntimeException("Strategy not found"));

            strategy.setStrategyType("AUTO_TRADING");
            strategy.setTargetAccountId(targetAccountId);
            strategy.setAssetType(assetType != null ? assetType : "KR_STOCK");
            strategy.setSymbol(symbol);

            // Set conditions (JSON strings from frontend)
            strategy.setEntryConditions(entryConditions);
            strategy.setExitConditions(exitConditions);

            // Risk management
            strategy.setStopLossType(stopLossType != null ? stopLossType : "NONE");
            strategy.setStopLossValue(stopLossValue);
            strategy.setTakeProfitType(takeProfitType != null ? takeProfitType : "NONE");
            strategy.setTakeProfitValue(takeProfitValue);

            // Position sizing
            strategy.setPositionSizeType(positionSizeType != null ? positionSizeType : "FIXED_AMOUNT");
            strategy.setPositionSizeValue(positionSizeValue);
            strategy.setMaxPositions(maxPositions != null ? maxPositions : 1);

            strategyRepository.save(strategy);

            redirectAttributes.addFlashAttribute("success", "Trading configuration updated");
            return "redirect:/trading/strategies/" + id + "/trading";

        } catch (Exception e) {
            log.error("Failed to update trading config", e);
            redirectAttributes.addFlashAttribute("error", "Failed to update: " + e.getMessage());
            return "redirect:/trading/strategies/" + id + "/trading";
        }
    }

    /**
     * Activate auto-trading
     */
    @PostMapping("/{id}/activate")
    public String activateTrading(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            tradingStrategyService.activateTrading(id);
            redirectAttributes.addFlashAttribute("success", "Auto-trading activated");
        } catch (Exception e) {
            log.error("Failed to activate trading", e);
            redirectAttributes.addFlashAttribute("error", "Activation failed: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    /**
     * Deactivate auto-trading
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateTrading(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            tradingStrategyService.deactivateTrading(id);
            redirectAttributes.addFlashAttribute("success", "Auto-trading deactivated");
        } catch (Exception e) {
            log.error("Failed to deactivate trading", e);
            redirectAttributes.addFlashAttribute("error", "Deactivation failed: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    /**
     * Sync strategy with Trading System
     */
    @PostMapping("/{id}/sync")
    public String syncStrategy(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            tradingStrategyService.syncToTradingSystem(id);
            redirectAttributes.addFlashAttribute("success", "Strategy synced successfully");
        } catch (Exception e) {
            log.error("Failed to sync strategy", e);
            redirectAttributes.addFlashAttribute("error", "Sync failed: " + e.getMessage());
        }
        return "redirect:/trading/strategies";
    }

    /**
     * View strategy monitoring/status
     */
    @GetMapping("/{id}/monitor")
    public String monitorStrategy(@PathVariable Long id, Model model,
                                 RedirectAttributes redirectAttributes) {
        Strategy strategy = strategyService.getStrategyById(id)
                .orElse(null);
        if (strategy == null) {
            redirectAttributes.addFlashAttribute("error", "Strategy not found");
            return "redirect:/trading/strategies";
        }

        model.addAttribute("strategy", strategy);

        // Get execution status from Trading System
        if (strategy.getExternalStrategyId() != null) {
            try {
                Map<String, Object> executionStatus =
                        tradingStrategyService.getStrategyExecutionStatus(id);
                model.addAttribute("executionStatus", executionStatus);

                // Get recent trades
                if (strategy.getTargetAccountId() != null) {
                    Map<String, Object> orders =
                            tradingApiService.getOrders(strategy.getTargetAccountId());
                    model.addAttribute("recentOrders", orders.get("items"));
                }
            } catch (Exception e) {
                log.error("Failed to get execution status", e);
                model.addAttribute("statusError", "Unable to fetch execution status");
            }
        }

        return "strategy/monitor";
    }

    /**
     * 전략 수동 실행
     */
    @PostMapping("/{id}/execute")
    public String executeStrategy(@PathVariable Long id,
                                  @RequestParam String symbol,
                                  @RequestParam String accountId,
                                  RedirectAttributes redirectAttributes) {
        try {
            log.info("Executing strategy manually: id={}, symbol={}, accountId={}", id, symbol, accountId);

            Strategy strategy = strategyService.getStrategyById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + id));

            // externalStrategyId가 있는지 확인
            if (strategy.getExternalStrategyId() == null || strategy.getExternalStrategyId().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "전략이 Trading System에 동기화되지 않았습니다. 먼저 동기화하세요.");
                return "redirect:/trading/strategies/" + id + "/monitor";
            }

            // Trading System API 호출
            Map<String, Object> result = tradingStrategyService.executeStrategy(
                    strategy.getExternalStrategyId(),
                    symbol,
                    accountId
            );

            redirectAttributes.addFlashAttribute("message", "전략이 성공적으로 실행되었습니다.");
            return "redirect:/trading/strategies/" + id + "/monitor";

        } catch (Exception e) {
            log.error("Failed to execute strategy", e);
            redirectAttributes.addFlashAttribute("error", "전략 실행에 실패했습니다: " + e.getMessage());
            return "redirect:/trading/strategies/" + id + "/monitor";
        }
    }
}
