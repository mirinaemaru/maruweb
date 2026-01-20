package com.maru.trading.service;

import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.SchedulerStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 전략 스케줄러 관리 서비스
 * Trading System의 스케줄러 API를 호출하여 전략 실행 스케줄을 관리합니다.
 */
@Slf4j
@Service
public class SchedulerService {

    private final RestTemplate restTemplate;
    private final String tradingApiBaseUrl;

    public SchedulerService(
            RestTemplate restTemplate,
            @Value("${trading.api.base-url:http://localhost:8099}") String tradingApiBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.tradingApiBaseUrl = tradingApiBaseUrl;
    }

    /**
     * 스케줄러 상태 조회
     *
     * @return 스케줄러 상태 정보
     */
    public SchedulerStatusResponse getStatus() {
        log.info("[SchedulerService] Getting scheduler status");

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/status";

        try {
            ResponseEntity<SchedulerStatusResponse> response = restTemplate.getForEntity(
                    url,
                    SchedulerStatusResponse.class
            );

            SchedulerStatusResponse statusResponse = response.getBody();
            log.info("[SchedulerService] Scheduler status: enabled={}, status={}, activeStrategies={}",
                    statusResponse != null ? statusResponse.isEnabled() : false,
                    statusResponse != null ? statusResponse.getStatus() : "N/A",
                    statusResponse != null ? statusResponse.getActiveStrategiesCount() : 0
            );

            return statusResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to get scheduler status", e);
            // 연결 실패 시 기본 응답 반환
            return SchedulerStatusResponse.builder()
                    .enabled(false)
                    .status("UNKNOWN")
                    .activeStrategiesCount(0)
                    .message("Trading System API에 연결할 수 없습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 스케줄러 활성화
     *
     * @return 응답 결과
     */
    public AckResponse enable() {
        log.info("[SchedulerService] Enabling scheduler");

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/enable";

        try {
            ResponseEntity<AckResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    AckResponse.class
            );

            AckResponse ackResponse = response.getBody();
            log.info("[SchedulerService] Enable scheduler response: {}", ackResponse);

            return ackResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to enable scheduler", e);
            return AckResponse.builder()
                    .ok(false)
                    .message("스케줄러 활성화에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 스케줄러 비활성화
     *
     * @return 응답 결과
     */
    public AckResponse disable() {
        log.info("[SchedulerService] Disabling scheduler");

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/disable";

        try {
            ResponseEntity<AckResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    AckResponse.class
            );

            AckResponse ackResponse = response.getBody();
            log.info("[SchedulerService] Disable scheduler response: {}", ackResponse);

            return ackResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to disable scheduler", e);
            return AckResponse.builder()
                    .ok(false)
                    .message("스케줄러 비활성화에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 특정 전략 수동 실행
     *
     * @param strategyId 실행할 전략 ID
     * @return 응답 결과
     */
    public AckResponse trigger(String strategyId) {
        if (strategyId == null || strategyId.isEmpty()) {
            throw new IllegalArgumentException("Strategy ID cannot be null or empty");
        }

        log.info("[SchedulerService] Triggering strategy: {}", strategyId);

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/trigger";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("strategyId", strategyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<AckResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AckResponse.class
            );

            AckResponse ackResponse = response.getBody();
            log.info("[SchedulerService] Trigger strategy response: {}", ackResponse);

            return ackResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to trigger strategy: {}", strategyId, e);
            return AckResponse.builder()
                    .ok(false)
                    .message("전략 실행에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 모든 활성 전략 즉시 실행
     *
     * @return 응답 결과
     */
    public AckResponse executeAll() {
        log.info("[SchedulerService] Executing all active strategies");

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/execute-all";

        try {
            ResponseEntity<AckResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    AckResponse.class
            );

            AckResponse ackResponse = response.getBody();
            log.info("[SchedulerService] Execute all response: {}", ackResponse);

            return ackResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to execute all strategies", e);
            return AckResponse.builder()
                    .ok(false)
                    .message("전체 전략 실행에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 실행 통계 초기화
     *
     * @return 응답 결과
     */
    public AckResponse resetStats() {
        log.info("[SchedulerService] Resetting scheduler stats");

        String url = tradingApiBaseUrl + "/api/v1/admin/scheduler/reset-stats";

        try {
            ResponseEntity<AckResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    AckResponse.class
            );

            AckResponse ackResponse = response.getBody();
            log.info("[SchedulerService] Reset stats response: {}", ackResponse);

            return ackResponse;
        } catch (RestClientException e) {
            log.error("[SchedulerService] Failed to reset stats", e);
            return AckResponse.builder()
                    .ok(false)
                    .message("통계 초기화에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }
}
