package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 스케줄러 실행 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionStats {

    /**
     * 총 실행 횟수
     */
    private int totalExecutions;

    /**
     * 성공 횟수
     */
    private int successfulExecutions;

    /**
     * 실패 횟수
     */
    private int failedExecutions;

    /**
     * 성공률 (%)
     */
    private double successRate;

    /**
     * 평균 실행 시간 (ms)
     */
    private long avgExecutionTimeMs;
}
