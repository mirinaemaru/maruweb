package com.maru.trading.controller;

import com.maru.trading.controller.TestConfig;
import com.maru.trading.service.NotificationService;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(DemoSignalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DemoSignalController 단위 테스트")
class DemoSignalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("데모 신호 페이지")
    void signalPage() throws Exception {
        Map<String, Object> accountsResponse = new HashMap<>();
        accountsResponse.put("items", Arrays.asList());
        when(tradingApiService.getAccounts()).thenReturn(accountsResponse);

        mockMvc.perform(get("/trading/demo-signal"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-signal"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("데모 신호 페이지 - API 오류")
    void signalPage_ApiError() throws Exception {
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/demo-signal"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-signal"))
                .andExpect(model().attribute("apiConnected", false))
                .andExpect(model().attributeExists("error"));
    }
}
