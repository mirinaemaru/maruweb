package com.maru.strategy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategyDto {
    private Long id;
    private String title;
    private String description;
    private String targetAccountId;
    private String assetType;
    private String symbol;                // Stock symbol (e.g., "005930" for Samsung)

    private List<TradingConditionDto> entryConditions;
    private List<TradingConditionDto> exitConditions;
    private RiskManagementDto riskManagement;
    private PositionSizingDto positionSizing;

    private String tradingStatus;
    private String syncStatus;
    private String externalStrategyId;
}
