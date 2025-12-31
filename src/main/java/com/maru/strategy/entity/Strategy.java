package com.maru.strategy.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "strategies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private StrategyCategory category;

    @Column(nullable = false, length = 20)
    private String status = "PLANNING";

    @Column(nullable = false, length = 20)
    private String priority = "MEDIUM";

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(nullable = false, length = 1)
    private String deleted = "N";

    // Auto-Trading Fields
    @Column(name = "strategy_type", length = 20)
    private String strategyType = "GENERAL";  // GENERAL or AUTO_TRADING

    @Column(name = "external_strategy_id", length = 100)
    private String externalStrategyId;

    @Column(name = "sync_status", length = 20)
    private String syncStatus = "NOT_SYNCED";  // NOT_SYNCED, SYNCED, OUT_OF_SYNC, ERROR

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "target_account_id", length = 100)
    private String targetAccountId;

    @Column(name = "asset_type", length = 20)
    private String assetType = "KR_STOCK";

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "entry_conditions", columnDefinition = "TEXT")
    private String entryConditions;

    @Column(name = "exit_conditions", columnDefinition = "TEXT")
    private String exitConditions;

    @Column(name = "stop_loss_type", length = 20)
    private String stopLossType = "NONE";

    @Column(name = "stop_loss_value")
    private Double stopLossValue;

    @Column(name = "take_profit_type", length = 20)
    private String takeProfitType = "NONE";

    @Column(name = "take_profit_value")
    private Double takeProfitValue;

    @Column(name = "position_size_type", length = 20)
    private String positionSizeType = "FIXED_AMOUNT";

    @Column(name = "position_size_value")
    private Double positionSizeValue;

    @Column(name = "max_positions")
    private Integer maxPositions = 1;

    @Column(name = "trading_status", length = 20)
    private String tradingStatus = "INACTIVE";

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "total_trades")
    private Integer totalTrades = 0;

    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Column(name = "losing_trades")
    private Integer losingTrades = 0;

    @Column(name = "total_profit_loss")
    private Double totalProfitLoss = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null || status.isEmpty()) {
            status = "PLANNING";
        }
        if (priority == null || priority.isEmpty()) {
            priority = "MEDIUM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for auto-trading
    public boolean isAutoTradingStrategy() {
        return "AUTO_TRADING".equals(this.strategyType);
    }

    public boolean isTradingActive() {
        return "ACTIVE".equals(this.tradingStatus);
    }

    public boolean isSynced() {
        return "SYNCED".equals(this.syncStatus);
    }
}
