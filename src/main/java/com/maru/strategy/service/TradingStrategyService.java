package com.maru.strategy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.strategy.entity.Strategy;
import com.maru.strategy.repository.StrategyRepository;
import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradingStrategyService {

    private final StrategyRepository strategyRepository;
    private final TradingApiService tradingApiService;
    private final ObjectMapper objectMapper;

    /**
     * Convert Strategy entity to Trading System API payload
     */
    public Map<String, Object> convertToApiPayload(Strategy strategy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", strategy.getTitle());
        payload.put("description", strategy.getDescription());
        payload.put("accountId", strategy.getTargetAccountId());
        payload.put("symbol", strategy.getSymbol());
        payload.put("assetType", strategy.getAssetType());

        // Parse and add conditions
        payload.put("entryConditions", parseJsonConditions(strategy.getEntryConditions()));
        payload.put("exitConditions", parseJsonConditions(strategy.getExitConditions()));

        // Risk management
        Map<String, Object> risk = new HashMap<>();
        risk.put("stopLossType", strategy.getStopLossType());
        risk.put("stopLossValue", strategy.getStopLossValue());
        risk.put("takeProfitType", strategy.getTakeProfitType());
        risk.put("takeProfitValue", strategy.getTakeProfitValue());
        payload.put("riskManagement", risk);

        // Position sizing
        Map<String, Object> sizing = new HashMap<>();
        sizing.put("type", strategy.getPositionSizeType());
        sizing.put("value", strategy.getPositionSizeValue());
        sizing.put("maxPositions", strategy.getMaxPositions());
        payload.put("positionSizing", sizing);

        return payload;
    }

    /**
     * Sync strategy to external Trading System
     */
    @Transactional
    public Strategy syncToTradingSystem(Long strategyId) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(strategyId, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found"));

        if (!"AUTO_TRADING".equals(strategy.getStrategyType())) {
            throw new RuntimeException("Only AUTO_TRADING strategies can be synced");
        }

        try {
            Map<String, Object> payload = convertToApiPayload(strategy);

            Map<String, Object> response;
            if (strategy.getExternalStrategyId() == null) {
                // Create new strategy in Trading System
                response = tradingApiService.createStrategy(payload);
                strategy.setExternalStrategyId((String) response.get("id"));
            } else {
                // Update existing strategy
                response = tradingApiService.updateStrategy(
                        strategy.getExternalStrategyId(), payload);
            }

            strategy.setSyncStatus("SYNCED");
            strategy.setLastSyncAt(LocalDateTime.now());

            log.info("Strategy {} synced successfully to Trading System", strategyId);

            return strategyRepository.save(strategy);

        } catch (Exception e) {
            log.error("Failed to sync strategy to Trading System", e);
            strategy.setSyncStatus("ERROR");
            strategyRepository.save(strategy);
            throw new RuntimeException("Failed to sync strategy: " + e.getMessage(), e);
        }
    }

    /**
     * Activate trading for a strategy
     */
    @Transactional
    public Strategy activateTrading(Long strategyId) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(strategyId, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found"));

        // Validation
        validateStrategyForActivation(strategy);

        // Sync to Trading System if not synced
        if (!"SYNCED".equals(strategy.getSyncStatus())) {
            syncToTradingSystem(strategyId);
        }

        try {
            // Activate in Trading System
            Map<String, Object> response = tradingApiService.updateStrategyStatus(
                    strategy.getExternalStrategyId(), "ACTIVE");

            strategy.setTradingStatus("ACTIVE");
            strategy.setActivatedAt(LocalDateTime.now());

            log.info("Trading activated successfully for strategy {}", strategyId);

            return strategyRepository.save(strategy);

        } catch (Exception e) {
            log.error("Failed to activate trading", e);
            throw new RuntimeException("Failed to activate trading: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate trading for a strategy
     */
    @Transactional
    public Strategy deactivateTrading(Long strategyId) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(strategyId, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found"));

        try {
            if (strategy.getExternalStrategyId() != null) {
                // Deactivate in Trading System
                tradingApiService.updateStrategyStatus(
                        strategy.getExternalStrategyId(), "INACTIVE");
            }

            strategy.setTradingStatus("INACTIVE");
            strategy.setDeactivatedAt(LocalDateTime.now());

            log.info("Trading deactivated successfully for strategy {}", strategyId);

            return strategyRepository.save(strategy);

        } catch (Exception e) {
            log.error("Failed to deactivate trading", e);
            throw new RuntimeException("Failed to deactivate trading: " + e.getMessage(), e);
        }
    }

    /**
     * Get strategy execution status from Trading System
     */
    public Map<String, Object> getStrategyExecutionStatus(Long strategyId) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(strategyId, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found"));

        if (strategy.getExternalStrategyId() == null) {
            return Collections.emptyMap();
        }

        try {
            return tradingApiService.getStrategy(strategy.getExternalStrategyId());
        } catch (Exception e) {
            log.error("Failed to get strategy status", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Validate strategy before activation
     */
    private void validateStrategyForActivation(Strategy strategy) {
        List<String> errors = new ArrayList<>();

        if (!"AUTO_TRADING".equals(strategy.getStrategyType())) {
            errors.add("Strategy type must be AUTO_TRADING");
        }

        if (strategy.getTargetAccountId() == null || strategy.getTargetAccountId().isEmpty()) {
            errors.add("Target account must be selected");
        }

        if (strategy.getSymbol() == null || strategy.getSymbol().isEmpty()) {
            errors.add("Trading symbol must be specified");
        }

        if (strategy.getEntryConditions() == null || strategy.getEntryConditions().isEmpty()) {
            errors.add("Entry conditions must be defined");
        }

        if (strategy.getPositionSizeValue() == null || strategy.getPositionSizeValue() <= 0) {
            errors.add("Position size must be greater than 0");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Helper: Parse JSON conditions
     */
    private List<Map<String, Object>> parseJsonConditions(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse conditions JSON", e);
            return Collections.emptyList();
        }
    }
}
