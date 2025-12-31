package com.maru.strategy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionSizingDto {
    private String type;              // FIXED_AMOUNT, PERCENTAGE_OF_BALANCE, SHARES
    private Double value;
    private Integer maxPositions;
}
