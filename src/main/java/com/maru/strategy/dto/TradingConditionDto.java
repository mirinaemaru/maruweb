package com.maru.strategy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingConditionDto {
    private String type;        // PRICE, INDICATOR_MA, INDICATOR_RSI, VOLUME, etc.
    private String operator;    // >=, <=, ==, CROSS_UP, CROSS_DOWN
    private String field;       // current_price, ma_5, ma_20, rsi_14, volume
    private Double value;
    private String timeframe;   // 1m, 5m, 1d, etc.
}
