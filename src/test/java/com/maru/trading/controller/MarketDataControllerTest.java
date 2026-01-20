package com.maru.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.trading.dto.*;
import com.maru.trading.service.MarketDataService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(MarketDataController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("MarketDataController 단위 테스트")
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MarketDataService marketDataService;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    @Nested
    @DisplayName("구독 종목 관리 페이지")
    class MarketDataPageTest {

        @Test
        @DisplayName("성공: 구독 종목 관리 페이지 렌더링")
        void marketDataPage_Success() throws Exception {
            // Given
            List<String> symbols = Arrays.asList("005490", "000270");
            SubscribedSymbolsResponse symbolsResponse = SubscribedSymbolsResponse.builder()
                    .symbols(symbols)
                    .total(2)
                    .subscriptionId("sub-123")
                    .active(true)
                    .build();

            MarketDataStatusResponse statusResponse = MarketDataStatusResponse.builder()
                    .subscribed(true)
                    .subscriptionId("sub-123")
                    .symbolCount(2)
                    .connected(true)
                    .message("Active subscription")
                    .build();

            when(marketDataService.getSubscribedSymbols()).thenReturn(symbolsResponse);
            when(marketDataService.getStatus()).thenReturn(statusResponse);

            // When & Then
            mockMvc.perform(get("/trading/market-data"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/market-data"))
                    .andExpect(model().attributeExists("symbols"))
                    .andExpect(model().attributeExists("status"));
        }

        @Test
        @DisplayName("실패: 서비스 에러 시 에러 메시지 표시")
        void marketDataPage_ServiceError() throws Exception {
            // Given
            when(marketDataService.getSubscribedSymbols()).thenThrow(new RuntimeException("API error"));

            // When & Then
            mockMvc.perform(get("/trading/market-data"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/market-data"))
                    .andExpect(model().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("종목 추가")
    class AddSymbolsTest {

        @Test
        @DisplayName("성공: 종목 추가 후 리다이렉트")
        void addSymbols_Success() throws Exception {
            // Given
            AddSymbolsRequest request = AddSymbolsRequest.builder()
                    .symbols(Arrays.asList("005490", "000270"))
                    .build();

            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Added 2 symbols to subscription")
                    .build();

            when(marketDataService.addSymbols(anyList())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/market-data/add")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("symbols", "005490,000270"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("message"));

            verify(marketDataService, times(1)).addSymbols(anyList());
        }

        @Test
        @DisplayName("실패: 빈 종목 목록으로 추가 시 에러")
        void addSymbols_EmptySymbols() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/market-data/add")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("symbols", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("error"));

            verify(marketDataService, never()).addSymbols(anyList());
        }

        @Test
        @DisplayName("실패: 서비스 에러 시 에러 메시지와 함께 리다이렉트")
        void addSymbols_ServiceError() throws Exception {
            // Given
            when(marketDataService.addSymbols(anyList())).thenThrow(new RuntimeException("API error"));

            // When & Then
            mockMvc.perform(post("/trading/market-data/add")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("symbols", "005490,000270"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("종목 삭제")
    class RemoveSymbolsTest {

        @Test
        @DisplayName("성공: 종목 삭제 후 리다이렉트")
        void removeSymbols_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Removed 1 symbols from subscription")
                    .build();

            when(marketDataService.removeSymbols(anyList())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/market-data/remove")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("symbols", "005490"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("message"));

            verify(marketDataService, times(1)).removeSymbols(anyList());
        }

        @Test
        @DisplayName("실패: 빈 종목 목록으로 삭제 시 에러")
        void removeSymbols_EmptySymbols() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/market-data/remove")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("symbols", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("error"));

            verify(marketDataService, never()).removeSymbols(anyList());
        }
    }

    @Nested
    @DisplayName("재구독")
    class ResubscribeTest {

        @Test
        @DisplayName("성공: 재구독 후 리다이렉트")
        void resubscribe_Success() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Successfully resubscribed to market data")
                    .build();

            when(marketDataService.resubscribe()).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/market-data/resubscribe"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("message"));

            verify(marketDataService, times(1)).resubscribe();
        }

        @Test
        @DisplayName("실패: 서비스 에러 시 에러 메시지와 함께 리다이렉트")
        void resubscribe_ServiceError() throws Exception {
            // Given
            when(marketDataService.resubscribe()).thenThrow(new RuntimeException("API error"));

            // When & Then
            mockMvc.perform(post("/trading/market-data/resubscribe"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("error"));
        }
    }
}
