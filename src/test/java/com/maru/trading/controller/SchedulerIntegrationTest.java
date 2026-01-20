package com.maru.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.ExecutionStats;
import com.maru.trading.dto.SchedulerStatusResponse;
import com.maru.trading.service.SchedulerService;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Scheduler 기능 통합 테스트
 * Controller → Service 전체 플로우 테스트 (RestTemplate은 Mock)
 *
 * 실제 외부 API 호출 없이 Controller와 Service 계층의 통합을 테스트합니다.
 */
@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import({SchedulerController.class})
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Scheduler 통합 테스트")
class SchedulerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchedulerService schedulerService;

    @MockBean
    private TradingApiService tradingApiService;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("스케줄러 관리 페이지")
    class SchedulerPageTests {

        @Test
        @DisplayName("성공: 페이지 로드 시 스케줄러 상태와 전략 목록 조회")
        void schedulerPage_LoadsStatusAndStrategies() throws Exception {
            // Given
            ExecutionStats stats = ExecutionStats.builder()
                    .totalExecutions(100)
                    .successfulExecutions(95)
                    .failedExecutions(5)
                    .successRate(95.0)
                    .avgExecutionTimeMs(230)
                    .build();

            SchedulerStatusResponse statusResponse = SchedulerStatusResponse.builder()
                    .enabled(true)
                    .status("RUNNING")
                    .activeStrategiesCount(3)
                    .lastExecutionTime("2026-01-20T10:30:00")
                    .nextExecutionTime("2026-01-20T10:35:00")
                    .executionStats(stats)
                    .build();

            Map<String, Object> strategiesResponse = new HashMap<>();
            List<Map<String, Object>> strategies = Arrays.asList(
                    createStrategy("strategy-1", "모멘텀 전략"),
                    createStrategy("strategy-2", "평균회귀 전략"),
                    createStrategy("strategy-3", "추세추종 전략")
            );
            strategiesResponse.put("strategies", strategies);

            when(schedulerService.getStatus()).thenReturn(statusResponse);
            when(tradingApiService.getStrategies()).thenReturn(strategiesResponse);

            // When & Then
            mockMvc.perform(get("/trading/scheduler"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/scheduler"))
                    .andExpect(model().attributeExists("status"))
                    .andExpect(model().attributeExists("strategies"));
        }

        @Test
        @DisplayName("성공: 비활성화 상태에서 페이지 로드")
        void schedulerPage_DisabledState() throws Exception {
            // Given
            SchedulerStatusResponse statusResponse = SchedulerStatusResponse.builder()
                    .enabled(false)
                    .status("STOPPED")
                    .activeStrategiesCount(0)
                    .build();

            when(schedulerService.getStatus()).thenReturn(statusResponse);
            when(tradingApiService.getStrategies()).thenReturn(new HashMap<>());

            // When & Then
            mockMvc.perform(get("/trading/scheduler"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/scheduler"))
                    .andExpect(model().attributeExists("status"));
        }

        @Test
        @DisplayName("실패: API 오류 시 에러 메시지 표시")
        void schedulerPage_ApiError_ShowsErrorMessage() throws Exception {
            // Given
            when(schedulerService.getStatus()).thenThrow(new RuntimeException("API connection failed"));

            // When & Then
            mockMvc.perform(get("/trading/scheduler"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/scheduler"))
                    .andExpect(model().attributeExists("error"));
        }

        private Map<String, Object> createStrategy(String id, String name) {
            Map<String, Object> strategy = new HashMap<>();
            strategy.put("id", id);
            strategy.put("name", name);
            return strategy;
        }
    }

    @Nested
    @DisplayName("스케줄러 활성화/비활성화")
    class SchedulerToggleTests {

        @Test
        @DisplayName("성공: 스케줄러 활성화")
        void enableScheduler_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Scheduler enabled successfully")
                    .build();

            when(schedulerService.enable()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/enable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("성공: 스케줄러 비활성화")
        void disableScheduler_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Scheduler disabled successfully")
                    .build();

            when(schedulerService.disable()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/disable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("실패: 활성화 실패 시 에러 메시지")
        void enableScheduler_Failure() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Cannot enable: no active strategies")
                    .build();

            when(schedulerService.enable()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/enable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("전략 실행")
    class StrategyExecutionTests {

        @Test
        @DisplayName("성공: 특정 전략 수동 실행")
        void triggerStrategy_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Strategy strategy-123 triggered successfully")
                    .build();

            when(schedulerService.trigger(anyString())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .param("strategyId", "strategy-123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("성공: 모든 활성 전략 실행")
        void executeAllStrategies_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Executed 5 active strategies")
                    .build();

            when(schedulerService.executeAll()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/execute-all"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 전략 실행")
        void triggerStrategy_NotFound() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Strategy not found: invalid-id")
                    .build();

            when(schedulerService.trigger(anyString())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .param("strategyId", "invalid-id"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("실패: 전략 ID 없이 실행 요청")
        void triggerStrategy_NoStrategyId() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("통계 관리")
    class StatsManagementTests {

        @Test
        @DisplayName("성공: 통계 초기화")
        void resetStats_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Stats reset successfully")
                    .build();

            when(schedulerService.resetStats()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/reset-stats"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("실패: 통계 초기화 실패")
        void resetStats_Failure() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Failed to reset stats: scheduler is running")
                    .build();

            when(schedulerService.resetStats()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/reset-stats"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }
}
