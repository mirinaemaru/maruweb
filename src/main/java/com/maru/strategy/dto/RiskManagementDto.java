package com.maru.strategy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskManagementDto {
    private String stopLossType;      // NONE, PERCENTAGE, FIXED_PRICE
    private Double stopLossValue;
    private String takeProfitType;    // NONE, PERCENTAGE, FIXED_PRICE
    private Double takeProfitValue;
}
