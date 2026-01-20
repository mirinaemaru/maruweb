package com.maru.trading.service;

import com.maru.trading.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService 단위 테스트")
class MarketDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private MarketDataService marketDataService;

    private static final String BASE_URL = "http://localhost:8099";

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(restTemplate, BASE_URL);
    }

    @Nested
    @DisplayName("종목 추가")
    class AddSymbolsTest {

        @Test
        @DisplayName("성공: 종목 추가 성공 시 성공 응답 반환")
        void addSymbols_Success() {
            // Given
            List<String> symbols = Arrays.asList("005490", "000270");
            AddSymbolsRequest request = AddSymbolsRequest.builder()
                    .symbols(symbols)
                    .build();

            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Added 2 symbols to subscription")
                    .build();

            when(restTemplate.exchange(
                    eq(BASE_URL + "/api/v1/admin/market-data/symbols"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = marketDataService.addSymbols(symbols);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("Added 2 symbols");

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            );
        }

        @Test
        @DisplayName("실패: 빈 종목 목록으로 추가 시 예외 발생")
        void addSymbols_EmptyList_ThrowsException() {
            // Given
            List<String> symbols = Arrays.asList();

            // When & Then
            assertThatThrownBy(() -> marketDataService.addSymbols(symbols))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbols cannot be empty");

            verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
        }

        @Test
        @DisplayName("실패: null 종목 목록으로 추가 시 예외 발생")
        void addSymbols_NullList_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> marketDataService.addSymbols(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbols cannot be null");

            verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
        }
    }

    @Nested
    @DisplayName("종목 삭제")
    class RemoveSymbolsTest {

        @Test
        @DisplayName("성공: 종목 삭제 성공 시 성공 응답 반환")
        void removeSymbols_Success() {
            // Given
            List<String> symbols = Arrays.asList("005490");
            RemoveSymbolsRequest request = RemoveSymbolsRequest.builder()
                    .symbols(symbols)
                    .build();

            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Removed 1 symbols from subscription")
                    .build();

            when(restTemplate.exchange(
                    eq(BASE_URL + "/api/v1/admin/market-data/symbols"),
                    eq(HttpMethod.DELETE),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = marketDataService.removeSymbols(symbols);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("Removed 1 symbols");

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.DELETE),
                    any(HttpEntity.class),
                    eq(AckResponse.class)
            );
        }
    }

    @Nested
    @DisplayName("구독 종목 조회")
    class GetSubscribedSymbolsTest {

        @Test
        @DisplayName("성공: 구독 종목 목록 반환")
        void getSubscribedSymbols_Success() {
            // Given
            List<String> symbols = Arrays.asList("005490", "000270", "005380");
            SubscribedSymbolsResponse expectedResponse = SubscribedSymbolsResponse.builder()
                    .symbols(symbols)
                    .total(3)
                    .subscriptionId("sub-123")
                    .active(true)
                    .build();

            when(restTemplate.getForEntity(
                    eq(BASE_URL + "/api/v1/admin/market-data/symbols"),
                    eq(SubscribedSymbolsResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SubscribedSymbolsResponse response = marketDataService.getSubscribedSymbols();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSymbols()).hasSize(3);
            assertThat(response.getTotal()).isEqualTo(3);
            assertThat(response.isActive()).isTrue();
            assertThat(response.getSubscriptionId()).isEqualTo("sub-123");

            verify(restTemplate, times(1)).getForEntity(
                    anyString(),
                    eq(SubscribedSymbolsResponse.class)
            );
        }

        @Test
        @DisplayName("성공: 구독 종목이 없을 때 빈 목록 반환")
        void getSubscribedSymbols_EmptyList() {
            // Given
            SubscribedSymbolsResponse expectedResponse = SubscribedSymbolsResponse.builder()
                    .symbols(Arrays.asList())
                    .total(0)
                    .subscriptionId(null)
                    .active(false)
                    .build();

            when(restTemplate.getForEntity(
                    eq(BASE_URL + "/api/v1/admin/market-data/symbols"),
                    eq(SubscribedSymbolsResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            SubscribedSymbolsResponse response = marketDataService.getSubscribedSymbols();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSymbols()).isEmpty();
            assertThat(response.getTotal()).isEqualTo(0);
            assertThat(response.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("구독 상태 조회")
    class GetStatusTest {

        @Test
        @DisplayName("성공: 구독 상태 반환")
        void getStatus_Success() {
            // Given
            MarketDataStatusResponse expectedResponse = MarketDataStatusResponse.builder()
                    .subscribed(true)
                    .subscriptionId("sub-123")
                    .symbolCount(5)
                    .connected(true)
                    .message("Active subscription with 5 symbols")
                    .build();

            when(restTemplate.getForEntity(
                    eq(BASE_URL + "/api/v1/admin/market-data/status"),
                    eq(MarketDataStatusResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            MarketDataStatusResponse response = marketDataService.getStatus();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSubscribed()).isTrue();
            assertThat(response.isConnected()).isTrue();
            assertThat(response.getSymbolCount()).isEqualTo(5);
            assertThat(response.getMessage()).contains("Active subscription");

            verify(restTemplate, times(1)).getForEntity(
                    anyString(),
                    eq(MarketDataStatusResponse.class)
            );
        }
    }

    @Nested
    @DisplayName("재구독")
    class ResubscribeTest {

        @Test
        @DisplayName("성공: 재구독 성공 시 성공 응답 반환")
        void resubscribe_Success() {
            // Given
            AckResponse expectedResponse = AckResponse.builder()
                    .ok(true)
                    .message("Successfully resubscribed to market data")
                    .build();

            when(restTemplate.postForEntity(
                    eq(BASE_URL + "/api/v1/admin/market-data/resubscribe"),
                    isNull(),
                    eq(AckResponse.class)
            )).thenReturn(ResponseEntity.ok(expectedResponse));

            // When
            AckResponse response = marketDataService.resubscribe();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOk()).isTrue();
            assertThat(response.getMessage()).contains("resubscribed");

            verify(restTemplate, times(1)).postForEntity(
                    anyString(),
                    isNull(),
                    eq(AckResponse.class)
            );
        }
    }
}
