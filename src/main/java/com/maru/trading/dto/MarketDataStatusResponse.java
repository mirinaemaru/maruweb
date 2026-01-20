package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 마켓 데이터 구독 상태 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDataStatusResponse {

    /**
     * 구독 여부
     */
    private boolean subscribed;

    /**
     * 구독 ID
     */
    private String subscriptionId;

    /**
     * 종목 수
     */
    private int symbolCount;

    /**
     * 연결 상태
     */
    private boolean connected;

    /**
     * 상태 메시지
     */
    private String message;
}
