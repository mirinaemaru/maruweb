package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(DemoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DemoController 단위 테스트")
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockDemoResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Demo executed successfully");
        result.put("success", true);
        return result;
    }

    @Test
    @DisplayName("데모 시나리오 페이지")
    void scenariosPage() throws Exception {
        mockMvc.perform(get("/trading/demo-scenarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-scenarios"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("Golden Cross 시나리오 실행")
    void runGoldenCross_Success() throws Exception {
        when(tradingApiService.runGoldenCrossDemo(any())).thenReturn(createMockDemoResponse());

        mockMvc.perform(post("/trading/demo-scenarios/golden-cross"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Death Cross 시나리오 실행")
    void runDeathCross_Success() throws Exception {
        when(tradingApiService.runDeathCrossDemo(any())).thenReturn(createMockDemoResponse());

        mockMvc.perform(post("/trading/demo-scenarios/death-cross"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("RSI Oversold 시나리오 실행")
    void runRsiOversold_Success() throws Exception {
        when(tradingApiService.runRsiOversoldDemo(any())).thenReturn(createMockDemoResponse());

        mockMvc.perform(post("/trading/demo-scenarios/rsi-oversold"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("Volatile 시나리오 실행")
    void runVolatile_Success() throws Exception {
        when(tradingApiService.runVolatileDemo(any())).thenReturn(createMockDemoResponse());

        mockMvc.perform(post("/trading/demo-scenarios/volatile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("시나리오 실행 - API 오류")
    void runScenario_ApiError() throws Exception {
        when(tradingApiService.runGoldenCrossDemo(any()))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(post("/trading/demo-scenarios/golden-cross"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/demo-scenarios"))
                .andExpect(flash().attributeExists("error"));
    }
}
