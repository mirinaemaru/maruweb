package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DemoController 통합테스트")
class DemoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ==================== 데모 시나리오 목록 ====================

    @Test
    @DisplayName("데모 시나리오 목록 페이지 - 성공")
    void page_Success() throws Exception {
        mockMvc.perform(get("/trading/demo-scenarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-scenarios"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("데모 시나리오 목록 - 성공 메시지 표시")
    void page_WithSuccessMessage() throws Exception {
        mockMvc.perform(get("/trading/demo-scenarios")
                        .param("successMessage", "시나리오가 성공적으로 실행되었습니다."))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-scenarios"))
                .andExpect(model().attribute("message", "시나리오가 성공적으로 실행되었습니다."));
    }

    @Test
    @DisplayName("데모 시나리오 목록 - 에러 메시지 표시")
    void page_WithErrorMessage() throws Exception {
        mockMvc.perform(get("/trading/demo-scenarios")
                        .param("errorMessage", "시나리오 실행에 실패했습니다."))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-scenarios"))
                .andExpect(model().attribute("error", "시나리오 실행에 실패했습니다."));
    }

    // ==================== Golden Cross 시나리오 ====================

    @Test
    @DisplayName("Golden Cross 시나리오 - 성공")
    void runGoldenCross_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("Golden Cross가 감지되어 매수 신호 발생");
        when(tradingApiService.runGoldenCrossDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/golden-cross")
                        .param("accountId", "account-1")
                        .param("symbol", "005930"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Golden Cross 시나리오 - 실패")
    void runGoldenCross_Error() throws Exception {
        // Given
        when(tradingApiService.runGoldenCrossDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/golden-cross"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== Death Cross 시나리오 ====================

    @Test
    @DisplayName("Death Cross 시나리오 - 성공")
    void runDeathCross_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("Death Cross가 감지되어 매도 신호 발생");
        when(tradingApiService.runDeathCrossDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/death-cross")
                        .param("accountId", "account-1")
                        .param("symbol", "005930"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Death Cross 시나리오 - 실패")
    void runDeathCross_Error() throws Exception {
        // Given
        when(tradingApiService.runDeathCrossDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/death-cross"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== RSI Oversold 시나리오 ====================

    @Test
    @DisplayName("RSI Oversold 시나리오 - 성공")
    void runRsiOversold_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("RSI 과매도 구간 진입");
        when(tradingApiService.runRsiOversoldDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/rsi-oversold"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("RSI Oversold 시나리오 - 실패")
    void runRsiOversold_Error() throws Exception {
        // Given
        when(tradingApiService.runRsiOversoldDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/rsi-oversold"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== RSI Overbought 시나리오 ====================

    @Test
    @DisplayName("RSI Overbought 시나리오 - 성공")
    void runRsiOverbought_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("RSI 과매수 구간 진입");
        when(tradingApiService.runRsiOverboughtDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/rsi-overbought"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("RSI Overbought 시나리오 - 실패")
    void runRsiOverbought_Error() throws Exception {
        // Given
        when(tradingApiService.runRsiOverboughtDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/rsi-overbought"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== Volatile 시나리오 ====================

    @Test
    @DisplayName("Volatile 시나리오 - 성공")
    void runVolatile_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("변동성 급증 감지");
        when(tradingApiService.runVolatileDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/volatile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Volatile 시나리오 - 실패")
    void runVolatile_Error() throws Exception {
        // Given
        when(tradingApiService.runVolatileDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/volatile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== Stable 시나리오 ====================

    @Test
    @DisplayName("Stable 시나리오 - 성공")
    void runStable_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("안정적 시장 상태");
        when(tradingApiService.runStableDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/stable"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Stable 시나리오 - 실패")
    void runStable_Error() throws Exception {
        // Given
        when(tradingApiService.runStableDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/stable"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== Custom 시나리오 ====================

    @Test
    @DisplayName("Custom 시나리오 - 성공")
    void runCustom_Success() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("커스텀 시나리오 실행 완료");
        when(tradingApiService.runCustomDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/custom")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("scenarioName", "테스트 시나리오")
                        .param("accountId", "account-1")
                        .param("symbol", "005930")
                        .param("tradeCount", "20")
                        .param("scenarioType", "BULLISH")
                        .param("minPrice", "70000")
                        .param("maxPrice", "80000")
                        .param("description", "테스트 설명"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Custom 시나리오 - 기본값으로 실행")
    void runCustom_DefaultValues() throws Exception {
        // Given
        Map<String, Object> result = createDemoResult("커스텀 시나리오 실행 완료");
        when(tradingApiService.runCustomDemo(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/custom")
                        .param("scenarioName", "기본 시나리오"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Custom 시나리오 - 실패")
    void runCustom_Error() throws Exception {
        // Given
        when(tradingApiService.runCustomDemo(anyMap())).thenThrow(new RuntimeException("API 오류"));

        // When & Then
        mockMvc.perform(post("/trading/demo-scenarios/custom")
                        .param("scenarioName", "테스트 시나리오"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createDemoResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("executedTrades", 5);
        result.put("timestamp", "2024-01-01T10:00:00");
        return result;
    }
}
