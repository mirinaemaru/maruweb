package com.maru.trading.service;

import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.ExecutionStats;
import com.maru.trading.dto.SchedulerStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerService 단위 테스트")
class SchedulerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SchedulerService schedulerService;

    private static final String BASE_URL = "http://localhost:8099";

    @BeforeEach
    void setUp() {
        schedulerService = new SchedulerService(restTemplate, BASE_URL);
    }

    @Nested
    @DisplayName("스케줄러 상태 조회")
    class GetStatusTest {

        @Test
        @DisplayName("성공: 스케줄러 상태 반환")
        void getStatus_Success() {
            // Given
            ExecutionStats stats = ExecutionStats.builder()
                    .totalExecutions(100)
                    .successfulExecutions(95)
                    .failedExecutions(5)
                    .successRate(95.0)
                    .avgExecutionTimeMs(230)
                    .build();

            SchedulerStatusResponse expectedResponse = SchedulerStatusResponse.builder()
                    .enabled(true)
                    .status("RUNNING")
                    .activeStrategiesCount(5)
                    .lastExecutionTime("2026-01-20T10:30:00")
                    .nextExecutionTime("2026-01-20T10:35:00")
                    .executionStats(stats)
                    .message("Scheduler is running")
                    .build();

            when(restTemplate.getForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/status"),
                    eq(SchedulerStatusResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SchedulerStatusResponse response = schedulerService.getStatus();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isTrue();
            assertThat(response.getStatus()).isEqualTo("RUNNING");
            assertThat(response.getActiveStrategiesCount()).isEqualTo(5);
            assertThat(response.getExecutionStats()).isNotNull();
            assertThat(response.getExecutionStats().getSuccessRate()).isEqualTo(95.0);

            verify(restTemplate, times(1)).getForEntity(
                    anyString(),
                    eq(SchedulerStatusResponse.class)
            );
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 기본 응답 반환")
        void getStatus_ConnectionFailed() {
            // Given
            when(restTemplate.getForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/status"),
                    eq(SchedulerStatusResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            SchedulerStatusResponse response = schedulerService.getStatus();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isEnabled()).isFalse();
            assertThat(response.getStatus()).isEqualTo("UNKNOWN");
            assertThat(response.getActiveStrategiesCount()).isEqualTo(0);
            assertThat(response.getMessage()).contains("연결할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("스케줄러 활성화")
    class EnableTest {

        @Test
        @DisplayName("성공: 스케줄러 활성화 성공 시 성공 응답 반환")
        void enable_Success() {
            // Given
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Scheduler enabled successfully")
                    .build();

            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/enable"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = schedulerService.enable();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("enabled");

            verify(restTemplate, times(1)).postForEntity(
                    anyString(),
                    isNull(),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 실패 응답 반환")
        void enable_ConnectionFailed() {
            // Given
            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/enable"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            AckResponse response = schedulerService.enable();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isFalse();
            assertThat(response.getMessage()).contains("실패");
        }
    }

    @Nested
    @DisplayName("스케줄러 비활성화")
    class DisableTest {

        @Test
        @DisplayName("성공: 스케줄러 비활성화 성공 시 성공 응답 반환")
        void disable_Success() {
            // Given
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Scheduler disabled successfully")
                    .build();

            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/disable"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = schedulerService.disable();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("disabled");

            verify(restTemplate, times(1)).postForEntity(
                    anyString(),
                    isNull(),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 실패 응답 반환")
        void disable_ConnectionFailed() {
            // Given
            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/disable"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            AckResponse response = schedulerService.disable();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isFalse();
            assertThat(response.getMessage()).contains("실패");
        }
    }

    @Nested
    @DisplayName("특정 전략 수동 실행")
    class TriggerTest {

        @Test
        @DisplayName("성공: 전략 실행 성공 시 성공 응답 반환")
        void trigger_Success() {
            // Given
            String strategyId = "strategy-123";
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Strategy triggered successfully")
                    .build();

            when(restTemplate.exchange(
                    eq(BASE_URL + "/api/v1/admin/scheduler/trigger"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = schedulerService.trigger(strategyId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("triggered");

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: null 전략 ID로 실행 시 예외 발생")
        void trigger_NullStrategyId_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> schedulerService.trigger(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");

            verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
        }

        @Test
        @DisplayName("실패: 빈 전략 ID로 실행 시 예외 발생")
        void trigger_EmptyStrategyId_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> schedulerService.trigger(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");

            verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 실패 응답 반환")
        void trigger_ConnectionFailed() {
            // Given
            String strategyId = "strategy-123";
            when(restTemplate.exchange(
                    eq(BASE_URL + "/api/v1/admin/scheduler/trigger"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            AckResponse response = schedulerService.trigger(strategyId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isFalse();
            assertThat(response.getMessage()).contains("실패");
        }
    }

    @Nested
    @DisplayName("모든 활성 전략 실행")
    class ExecuteAllTest {

        @Test
        @DisplayName("성공: 전체 실행 성공 시 성공 응답 반환")
        void executeAll_Success() {
            // Given
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("All strategies executed successfully")
                    .build();

            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/execute-all"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = schedulerService.executeAll();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("executed");

            verify(restTemplate, times(1)).postForEntity(
                    anyString(),
                    isNull(),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 실패 응답 반환")
        void executeAll_ConnectionFailed() {
            // Given
            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/execute-all"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            AckResponse response = schedulerService.executeAll();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isFalse();
            assertThat(response.getMessage()).contains("실패");
        }
    }

    @Nested
    @DisplayName("통계 초기화")
    class ResetStatsTest {

        @Test
        @DisplayName("성공: 통계 초기화 성공 시 성공 응답 반환")
        void resetStats_Success() {
            // Given
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Stats reset successfully")
                    .build();

            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/reset-stats"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = schedulerService.resetStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("reset");

            verify(restTemplate, times(1)).postForEntity(
                    anyString(),
                    isNull(),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: API 연결 실패 시 실패 응답 반환")
        void resetStats_ConnectionFailed() {
            // Given
            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/scheduler/reset-stats"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            AckResponse response = schedulerService.resetStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isFalse();
            assertThat(response.getMessage()).contains("실패");
        }
    }
}
