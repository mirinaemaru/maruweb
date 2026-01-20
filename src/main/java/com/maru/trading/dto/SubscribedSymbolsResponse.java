package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 구독 종목 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscribedSymbolsResponse {

    /**
     * 현재 구독 중인 종목 목록
     */
    private List<String> symbols;

    /**
     * 총 종목 수
     */
    private int total;

    /**
     * 구독 ID
     */
    private String subscriptionId;

    /**
     * 구독 활성화 여부
     */
    private boolean active;
}
