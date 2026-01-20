package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 구독 종목 삭제 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveSymbolsRequest {

    /**
     * 삭제할 종목 코드 목록
     * 예: ["005380", "051910"]
     */
    private List<String> symbols;
}
