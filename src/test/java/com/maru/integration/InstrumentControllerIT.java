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

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("InstrumentController 통합테스트")
class InstrumentControllerIT {

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

    // ========== 종목 목록 ==========

    @Test
    @DisplayName("종목 목록 조회 - 기본값 적용")
    void listInstruments_DefaultValues() throws Exception {
        // Given
        List<Map<String, Object>> instruments = new ArrayList<>();
        instruments.add(createInstrumentMock("005930", "삼성전자"));
        instruments.add(createInstrumentMock("000660", "SK하이닉스"));
        Map<String, Object> result = new HashMap<>();
        result.put("items", instruments);
        result.put("total", 2);

        when(tradingApiService.getInstruments("KOSPI", "LISTED", true, null)).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attribute("selectedMarket", "KOSPI"))
                .andExpect(model().attribute("selectedStatus", "LISTED"))
                .andExpect(model().attribute("selectedTradable", true));
    }

    @Test
    @DisplayName("종목 목록 조회 - 필터 적용")
    void listInstruments_WithFilters() throws Exception {
        // Given
        List<Map<String, Object>> instruments = new ArrayList<>();
        instruments.add(createInstrumentMock("035720", "카카오"));
        Map<String, Object> result = new HashMap<>();
        result.put("items", instruments);
        result.put("total", 1);

        when(tradingApiService.getInstruments("KOSDAQ", "LISTED", false, "카카오")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments")
                        .param("market", "KOSDAQ")
                        .param("status", "LISTED")
                        .param("tradable", "false")
                        .param("search", "카카오"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attribute("selectedMarket", "KOSDAQ"))
                .andExpect(model().attribute("selectedStatus", "LISTED"))
                .andExpect(model().attribute("selectedTradable", false))
                .andExpect(model().attribute("searchKeyword", "카카오"));
    }

    @Test
    @DisplayName("종목 목록 조회 - API 오류")
    void listInstruments_ApiError() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("items", Collections.emptyList());
        result.put("error", "API 연결 실패");

        when(tradingApiService.getInstruments(anyString(), anyString(), anyBoolean(), isNull())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instruments"))
                .andExpect(model().attribute("error", "API 연결 실패"));
    }

    // ========== 종목 상세 ==========

    @Test
    @DisplayName("종목 상세 조회 - 성공")
    void getInstrument_Success() throws Exception {
        // Given
        Map<String, Object> instrument = createInstrumentMock("005930", "삼성전자");
        when(tradingApiService.getInstrument("005930")).thenReturn(instrument);

        // When & Then
        mockMvc.perform(get("/trading/instruments/{symbol}", "005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instrument-detail"))
                .andExpect(model().attributeExists("instrument"));
    }

    @Test
    @DisplayName("종목 상세 조회 - 오류")
    void getInstrument_Error() throws Exception {
        // Given - 오류 응답이지만 템플릿에서 사용하는 기본 필드 포함
        Map<String, Object> result = createInstrumentMock("INVALID", "알수없음");
        result.put("error", "종목을 찾을 수 없습니다");
        when(tradingApiService.getInstrument("INVALID")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments/{symbol}", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/instrument-detail"))
                .andExpect(model().attribute("error", "종목을 찾을 수 없습니다"));
    }

    // ========== 종목 상태 업데이트 ==========

    @Test
    @DisplayName("종목 상태 업데이트 - 성공")
    void updateInstrumentStatus_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(tradingApiService.updateInstrumentStatus("005930", "LISTED", true, false)).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/status", "005930")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("status", "LISTED")
                        .param("tradable", "true")
                        .param("halted", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/instruments/005930"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("종목 상태 업데이트 - 오류")
    void updateInstrumentStatus_Error() throws Exception {
        // Given
        doThrow(new RuntimeException("업데이트 실패"))
                .when(tradingApiService).updateInstrumentStatus(eq("005930"), anyString(), anyBoolean(), anyBoolean());

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/status", "005930")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("status", "LISTED")
                        .param("tradable", "true")
                        .param("halted", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/instruments/005930"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ========== 거래 가능 상태 토글 (AJAX) ==========

    @Test
    @DisplayName("거래 가능 상태 토글 - 성공")
    void toggleTradable_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tradable", false);
        when(tradingApiService.updateInstrumentStatus("005930", null, false, null)).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/toggle-tradable", "005930")
                        .param("tradable", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("거래 가능 상태 토글 - 활성화")
    void toggleTradable_Enable() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tradable", true);
        when(tradingApiService.updateInstrumentStatus("005930", null, true, null)).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/toggle-tradable", "005930")
                        .param("tradable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradable").value(true));
    }

    // ========== 거래 정지 상태 토글 (AJAX) ==========

    @Test
    @DisplayName("거래 정지 상태 토글 - 정지 활성화")
    void toggleHalted_Enable() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("halted", true);
        when(tradingApiService.updateInstrumentStatus("005930", null, null, true)).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/toggle-halted", "005930")
                        .param("halted", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.halted").value(true));
    }

    @Test
    @DisplayName("거래 정지 상태 토글 - 정지 해제")
    void toggleHalted_Disable() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("halted", false);
        when(tradingApiService.updateInstrumentStatus("005930", null, null, false)).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/instruments/{symbol}/toggle-halted", "005930")
                        .param("halted", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.halted").value(false));
    }

    // ========== 종목 검색 API ==========

    @Test
    @DisplayName("종목 검색 API - 성공")
    void searchInstrumentsApi_Success() throws Exception {
        // Given
        List<Map<String, Object>> instruments = new ArrayList<>();
        instruments.add(createInstrumentMock("005930", "삼성전자"));
        instruments.add(createInstrumentMock("005935", "삼성전자우"));
        Map<String, Object> result = new HashMap<>();
        result.put("items", instruments);
        result.put("total", 2);

        when(tradingApiService.getInstruments("KOSPI", "LISTED", true, "삼성")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments/api/search")
                        .param("market", "KOSPI")
                        .param("search", "삼성"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("종목 검색 API - 빈 결과")
    void searchInstrumentsApi_EmptyResult() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("items", Collections.emptyList());
        result.put("total", 0);

        when(tradingApiService.getInstruments(null, "LISTED", true, "존재하지않는종목")).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/trading/instruments/api/search")
                        .param("search", "존재하지않는종목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createInstrumentMock(String symbol, String name) {
        Map<String, Object> instrument = new HashMap<>();
        instrument.put("symbol", symbol);
        instrument.put("name", name);
        instrument.put("nameKr", name);  // 한글 종목명 (템플릿에서 사용)
        instrument.put("nameEn", name + " Corp");  // 영문 종목명
        instrument.put("market", "KOSPI");
        instrument.put("status", "LISTED");
        instrument.put("tradable", true);
        instrument.put("halted", false);
        instrument.put("currentPrice", new BigDecimal("75000"));
        instrument.put("previousClose", new BigDecimal("74500"));
        instrument.put("changePercent", new BigDecimal("0.67"));
        instrument.put("volume", 10000000L);
        instrument.put("updatedAt", "2024-01-01T10:00:00");
        // 템플릿 instrument-detail.html에서 사용하는 추가 필드
        instrument.put("sectorCode", "IT");
        instrument.put("industry", "반도체");
        instrument.put("tickSize", new BigDecimal("100"));
        instrument.put("lotSize", 1);
        instrument.put("listingDate", "2010-01-04");
        instrument.put("delistingDate", null);
        instrument.put("createdAt", "2024-01-01T10:00:00");
        return instrument;
    }
}
