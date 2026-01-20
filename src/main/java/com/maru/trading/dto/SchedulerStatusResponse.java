package com.maru.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 스케줄러 상태 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerStatusResponse {

    /**
     * 스케줄러 활성화 여부
     */
    private boolean enabled;

    /**
     * 스케줄러 상태 (RUNNING, STOPPED, PAUSED)
     */
    private String status;

    /**
     * 활성 전략 수
     */
    private int activeStrategiesCount;

    /**
     * 마지막 실행 시간
     */
    private String lastExecutionTime;

    /**
     * 다음 예정 실행 시간
     */
    private String nextExecutionTime;

    /**
     * 실행 통계
     */
    private ExecutionStats executionStats;

    /**
     * 상태 메시지
     */
    private String message;
}
