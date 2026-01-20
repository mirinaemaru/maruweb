package com.maru.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.MarketDataStatusResponse;
import com.maru.trading.dto.SubscribedSymbolsResponse;
import com.maru.trading.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MarketData 기능 통합 테스트
 * Controller → Service 전체 플로우 테스트 (RestTemplate은 Mock)
 *
 * 실제 외부 API 호출 없이 Controller와 Service 계층의 통합을 테스트합니다.
 */
@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import({MarketDataController.class})
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("MarketData 통합 테스트")
class MarketDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("구독 종목 관리 페이지")
    class MarketDataPageTests {

        @Test
        @DisplayName("성공: 페이지 로드 시 구독 정보 조회")
        void marketDataPage_LoadsSubscriptionInfo() throws Exception {
            // Given
            SubscribedSymbolsResponse symbolsResponse = SubscribedSymbolsResponse.builder()
                    .symbols(Arrays.asList("005490", "000270"))
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
        @DisplayName("실패: API 오류 시 에러 메시지 표시")
        void marketDataPage_ApiError_ShowsErrorMessage() throws Exception {
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
    class AddSymbolsTests {

        @Test
        @DisplayName("성공: 종목 추가 후 리다이렉트")
        void addSymbols_Success_RedirectsWithMessage() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Added 2 symbols to subscription")
                    .build();

            when(marketDataService.addSymbols(anyList())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/market-data/add")
                            .param("symbols", "005490,000270"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("실패: 빈 종목 목록으로 추가 시 에러")
        void addSymbols_EmptySymbols_ShowsError() throws Exception {
            // When & Then
            mockMvc.perform(post("/trading/market-data/add")
                            .param("symbols", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("종목 삭제")
    class RemoveSymbolsTests {

        @Test
        @DisplayName("성공: 종목 삭제 후 리다이렉트")
        void removeSymbols_Success_RedirectsWithMessage() throws Exception {
            // Given
            AckResponse ackResponse = AckResponse.builder()
                    .ok(true)
                    .message("Removed 1 symbol from subscription")
                    .build();

            when(marketDataService.removeSymbols(anyList())).thenReturn(ackResponse);

            // When & Then
            mockMvc.perform(post("/trading/market-data/remove")
                            .param("symbols", "005490"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/trading/market-data"))
                    .andExpect(flash().attributeExists("message"));
        }
    }

    @Nested
    @DisplayName("재구독")
    class ResubscribeTests {

        @Test
        @DisplayName("성공: 재구독 후 리다이렉트")
        void resubscribe_Success_RedirectsWithMessage() throws Exception {
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
        }
    }
}
