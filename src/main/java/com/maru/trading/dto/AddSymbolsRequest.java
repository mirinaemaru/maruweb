package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 구독 종목 추가 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSymbolsRequest {

    /**
     * 추가할 종목 코드 목록
     * 예: ["005490", "000270"]
     */
    private List<String> symbols;
}
