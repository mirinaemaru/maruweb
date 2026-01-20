package com.maru.trading.controller;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(SchedulerController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("SchedulerController 단위 테스트")
class SchedulerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchedulerService schedulerService;

    @MockBean
    private TradingApiService tradingApiService;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    @Nested
    @DisplayName("스케줄러 관리 페이지")
    class SchedulerPageTest {

        @Test
        @DisplayName("성공: 스케줄러 관리 페이지 렌더링")
        void schedulerPage_Success() throws Exception {
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
                    .activeStrategiesCount(5)
                    .lastExecutionTime("2026-01-20T10:30:00")
                    .nextExecutionTime("2026-01-20T10:35:00")
                    .executionStats(stats)
                    .build();

            Map<String, Object> strategiesResponse = new HashMap<>();
            List<Map<String, Object>> strategies = Arrays.asList(
                    createStrategy("strategy-1", "전략 1"),
                    createStrategy("strategy-2", "전략 2")
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

            verify(schedulerService, times(1)).getStatus();
            verify(tradingApiService, times(1)).getStrategies();
        }

        @Test
        @DisplayName("실패: 서비스 에러 시 에러 메시지 표시")
        void schedulerPage_ServiceError() throws Exception {
            // Given
            when(schedulerService.getStatus()).thenThrow(new RuntimeException("API error"));

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
    @DisplayName("스케줄러 활성화")
    class EnableSchedulerTest {

        @Test
        @DisplayName("성공: 스케줄러 활성화 후 리다이렉트")
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

            verify(schedulerService, times(1)).enable();
        }

        @Test
        @DisplayName("실패: 서비스 실패 응답 시 에러 메시지와 함께 리다이렉트")
        void enableScheduler_ServiceFailed() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Failed to enable scheduler")
                    .build();

            when(schedulerService.enable()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/enable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("실패: 서비스 예외 시 에러 메시지와 함께 리다이렉트")
        void enableScheduler_ServiceError() throws Exception {
            // Given
            when(schedulerService.enable()).thenThrow(new RuntimeException("API error"));

            // When & Then
            mockMvc.perform(post("/trading/scheduler/enable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("스케줄러 비활성화")
    class DisableSchedulerTest {

        @Test
        @DisplayName("성공: 스케줄러 비활성화 후 리다이렉트")
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

            verify(schedulerService, times(1)).disable();
        }

        @Test
        @DisplayName("실패: 서비스 실패 응답 시 에러 메시지와 함께 리다이렉트")
        void disableScheduler_ServiceFailed() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Failed to disable scheduler")
                    .build();

            when(schedulerService.disable()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/disable"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("전략 수동 실행")
    class TriggerStrategyTest {

        @Test
        @DisplayName("성공: 전략 실행 후 리다이렉트")
        void triggerStrategy_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Strategy triggered successfully")
                    .build();

            when(schedulerService.trigger(anyString())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("strategyId", "strategy-123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));

            verify(schedulerService, times(1)).trigger("strategy-123");
        }

        @Test
        @DisplayName("실패: 빈 전략 ID로 실행 시 에러")
        void triggerStrategy_EmptyStrategyId() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("strategyId", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));

            verify(schedulerService, never()).trigger(anyString());
        }

        @Test
        @DisplayName("실패: 전략 ID 없이 실행 시 에러")
        void triggerStrategy_NoStrategyId() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));

            verify(schedulerService, never()).trigger(anyString());
        }

        @Test
        @DisplayName("실패: 서비스 실패 응답 시 에러 메시지와 함께 리다이렉트")
        void triggerStrategy_ServiceFailed() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Strategy not found")
                    .build();

            when(schedulerService.trigger(anyString())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("strategyId", "invalid-strategy"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("모든 전략 실행")
    class ExecuteAllTest {

        @Test
        @DisplayName("성공: 전체 실행 후 리다이렉트")
        void executeAllStrategies_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("All strategies executed")
                    .build();

            when(schedulerService.executeAll()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/scheduler/execute-all"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("message"));

            verify(schedulerService, times(1)).executeAll();
        }

        @Test
        @DisplayName("실패: 서비스 예외 시 에러 메시지와 함께 리다이렉트")
        void executeAllStrategies_ServiceError() throws Exception {
            // Given
            when(schedulerService.executeAll()).thenThrow(new RuntimeException("API error"));

            // When & Then
            mockMvc.perform(post("/trading/scheduler/execute-all"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/scheduler"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("통계 초기화")
    class ResetStatsTest {

        @Test
        @DisplayName("성공: 통계 초기화 후 리다이렉트")
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

            verify(schedulerService, times(1)).resetStats();
        }

        @Test
        @DisplayName("실패: 서비스 실패 응답 시 에러 메시지와 함께 리다이렉트")
        void resetStats_ServiceFailed() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(false)
                    .message("Failed to reset stats")
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
