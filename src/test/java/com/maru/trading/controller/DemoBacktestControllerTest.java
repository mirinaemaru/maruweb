package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(DemoBacktestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DemoBacktestController 단위 테스트")
class DemoBacktestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @Test
    @DisplayName("최적화 데모 페이지")
    void optimizationDemoPage() throws Exception {
        mockMvc.perform(get("/trading/demo/optimization"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/optimization-demo"));
    }

    @Test
    @DisplayName("데모 백테스트 페이지")
    void backtestDemoPage() throws Exception {
        mockMvc.perform(get("/trading/demo/backtest"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/demo-backtest"));
    }
}
