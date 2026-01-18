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
@DisplayName("DemoSignalController 통합테스트")
class DemoSignalControllerIT {

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

    // ========== 데모 신호 페이지 ==========

    @Test
    @DisplayName("데모 신호 페이지 - 성공")
    void page_Success() throws Exception {
        // Given
        Map<String, Object> accountsData = new HashMap<>();
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "테스트 계좌"));
        accountsData.put("items", accounts);
        when(tradingApiService.getAccounts()).thenReturn(accountsData);

        // When & Then
        mockMvc.perform(get("/trading/demo-signal"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-signal"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("데모 신호 페이지 - API 연결 실패")
    void page_ApiConnectionFailed() throws Exception {
        // Given
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/demo-signal"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-signal"))
                .andExpect(model().attribute("apiConnected", false))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("errorDetail"));
    }

    // ========== 데모 신호 주입 ==========

    @Test
    @DisplayName("데모 신호 주입 - 성공")
    void injectSignal_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("signalId", "signal-001");
        when(tradingApiService.injectDemoSignal(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-signal")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("accountId", "account-1")
                        .param("symbol", "005930")
                        .param("side", "BUY")
                        .param("targetType", "QTY")
                        .param("targetValue", "10")
                        .param("ttlSeconds", "60"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-signal"))
                .andExpect(flash().attributeExists("message"));

        verify(tradingApiService).injectDemoSignal(anyMap());
    }

    @Test
    @DisplayName("데모 신호 주입 - 계좌 ID 없이")
    void injectSignal_WithoutAccountId() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("signalId", "signal-002");
        when(tradingApiService.injectDemoSignal(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-signal")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbol", "000660")
                        .param("side", "SELL")
                        .param("targetValue", "50000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-signal"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("데모 신호 주입 - 실패")
    void injectSignal_Failed() throws Exception {
        // Given
        when(tradingApiService.injectDemoSignal(anyMap())).thenThrow(new RuntimeException("신호 주입 실패"));

        // When & Then
        mockMvc.perform(post("/trading/demo-signal")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbol", "005930")
                        .param("side", "BUY")
                        .param("targetValue", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-signal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("데모 신호 주입 - AMT 타입")
    void injectSignal_AmtType() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(tradingApiService.injectDemoSignal(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/demo-signal")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbol", "005930")
                        .param("side", "BUY")
                        .param("targetType", "AMT")
                        .param("targetValue", "1000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-signal"))
                .andExpect(flash().attributeExists("message"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createAccountMock(String accountId, String alias) {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", accountId);
        account.put("alias", alias);
        account.put("cano", "12345678");  // 계좌번호 (템플릿에서 사용)
        account.put("broker", "키움증권");
        account.put("status", "ACTIVE");
        return account;
    }
}
