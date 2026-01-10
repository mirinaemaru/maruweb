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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(InstrumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InstrumentController 단위 테스트")
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockInstrumentsResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> instrument1 = new HashMap<>();
        instrument1.put("symbol", "005930");
        instrument1.put("name", "삼성전자");
        instrument1.put("nameKr", "삼성전자");
        instrument1.put("nameEn", "Samsung Electronics");
        instrument1.put("market", "KOSPI");
        instrument1.put("status", "LISTED");
        instrument1.put("tradable", true);
        instrument1.put("halted", false);
        instrument1.put("currentPrice", 70000);
        instrument1.put("previousClose", 69000);
        instrument1.put("changePrice", 1000);
        instrument1.put("changeRate", 1.5);
        instrument1.put("volume", 10000000);
        instrument1.put("marketCap", 400000000000000L);
        instrument1.put("industry", "반도체");
        instrument1.put("sector", "전기전자");
        instrument1.put("sectorCode", "35");
        instrument1.put("listingDate", "1975-06-11");
        response.put("items", Arrays.asList(instrument1));
        response.put("total", 1);
        return response;
    }

    @Test
    @DisplayName("종목 관리 페이지 - 성공")
    void list_Success() throws Exception {
        when(tradingApiService.getInstruments(any(), any(), any(), any()))
                .thenReturn(createMockInstrumentsResponse());

        mockMvc.perform(get("/trading/instruments"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    @DisplayName("종목 검색 - 키워드 필터")
    void list_WithKeyword() throws Exception {
        when(tradingApiService.getInstruments(any(), any(), any(), any()))
                .thenReturn(createMockInstrumentsResponse());

        mockMvc.perform(get("/trading/instruments")
                        .param("search", "삼성")
                        .param("market", "KOSPI"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attribute("searchKeyword", "삼성"));
    }

    @Test
    @DisplayName("종목 상세 페이지 - 성공")
    void detail_Success() throws Exception {
        Map<String, Object> instrument = new HashMap<>();
        instrument.put("symbol", "005930");
        instrument.put("name", "삼성전자");
        instrument.put("nameKr", "삼성전자");
        instrument.put("nameEn", "Samsung Electronics");
        instrument.put("market", "KOSPI");
        instrument.put("status", "LISTED");
        instrument.put("tradable", true);
        instrument.put("halted", false);
        instrument.put("currentPrice", 70000);
        instrument.put("previousClose", 69000);
        instrument.put("changePrice", 1000);
        instrument.put("changeRate", 1.5);
        instrument.put("volume", 10000000);
        instrument.put("high", 71000);
        instrument.put("low", 69000);
        instrument.put("open", 69500);
        instrument.put("marketCap", 400000000000000L);
        instrument.put("per", 10.5);
        instrument.put("pbr", 1.2);
        instrument.put("industry", "반도체");
        instrument.put("sector", "전기전자");
        instrument.put("sectorCode", "35");
        instrument.put("listingDate", "1975-06-11");
        instrument.put("delistingDate", null);
        instrument.put("eps", 5000);
        instrument.put("bps", 55000);
        instrument.put("dividendYield", 2.5);
        instrument.put("tickSize", 100);
        instrument.put("lotSize", 1);
        instrument.put("currency", "KRW");
        instrument.put("createdAt", "2025-01-01T10:00:00");
        instrument.put("updatedAt", "2025-01-01T10:00:00");

        when(tradingApiService.getInstrument("005930")).thenReturn(instrument);

        mockMvc.perform(get("/trading/instruments/005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instrument-detail"))
                .andExpect(model().attributeExists("instrument"));
    }

    @Test
    @DisplayName("종목 관리 - API 오류 시")
    void list_ApiError() throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Connection refused");
        errorResponse.put("items", null);
        errorResponse.put("total", 0);

        when(tradingApiService.getInstruments(any(), any(), any(), any()))
                .thenReturn(errorResponse);

        mockMvc.perform(get("/trading/instruments"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attributeExists("error"));
    }
}
