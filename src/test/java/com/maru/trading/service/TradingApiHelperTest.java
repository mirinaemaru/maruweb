package com.maru.trading.service;

import com.maru.trading.exception.TradingApiException;
import com.maru.trading.exception.TradingApiException.ErrorCode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TradingApiHelper 단위 테스트
 * - HTTP 메서드별 동작 확인
 * - 에러 변환 확인
 * - Resilience4j 통합 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradingApiHelper 단위 테스트")
class TradingApiHelperTest {

    @Mock
    private RestTemplate tradingApiRestTemplate;

    private TradingApiHelper helper;
    private Retry retry;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // 테스트용 Retry 설정 (빠른 테스트를 위해 1회만 재시도)
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .build();
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        retry = retryRegistry.retry("testRetry");

        // 테스트용 Circuit Breaker 설정
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .build();
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        circuitBreaker = cbRegistry.circuitBreaker("testCB");

        helper = new TradingApiHelper(tradingApiRestTemplate, retry, circuitBreaker);
    }

    @Test
    @DisplayName("TradingApiHelper 인스턴스 생성 확인")
    void helperInstance_IsCreated() {
        assertThat(helper).isNotNull();
        assertThat(retry).isNotNull();
        assertThat(circuitBreaker).isNotNull();
    }

    // ==================== HTTP 메서드 테스트 ====================

    @Nested
    @DisplayName("GET 요청 테스트")
    class GetRequestTests {

        @Test
        @DisplayName("GET 요청 성공")
        void get_Success() {
            // Given
            Map<String, Object> expectedResponse = new HashMap<>();
            expectedResponse.put("status", "UP");

            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            // When
            Map<String, Object> result = helper.get("/health", ErrorCode.CONNECTION_FAILED);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("status")).isEqualTo("UP");
        }

        @Test
        @DisplayName("GET 요청 실패 시 기본값 반환")
        void getWithDefault_Failure_ReturnsDefault() {
            // Given
            Map<String, Object> defaultValue = new HashMap<>();
            defaultValue.put("status", "DOWN");

            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));

            // When
            Map<String, Object> result = helper.getWithDefault("/health", defaultValue);

            // Then
            assertThat(result).isEqualTo(defaultValue);
        }
    }

    @Nested
    @DisplayName("POST 요청 테스트")
    class PostRequestTests {

        @Test
        @DisplayName("POST 요청 성공")
        void post_Success() {
            // Given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", "test");

            Map<String, Object> expectedResponse = new HashMap<>();
            expectedResponse.put("id", "created-id");

            when(tradingApiRestTemplate.exchange(
                    eq("/api/create"),
                    eq(HttpMethod.POST),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            // When
            Map<String, Object> result = helper.post("/api/create", requestBody, ErrorCode.INTERNAL_ERROR);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("created-id");
        }

        @Test
        @DisplayName("POST 요청 실패 시 기본값 반환")
        void postWithDefault_Failure_ReturnsDefault() {
            // Given
            Map<String, Object> defaultValue = new HashMap<>();
            defaultValue.put("error", true);

            when(tradingApiRestTemplate.exchange(
                    eq("/api/create"),
                    eq(HttpMethod.POST),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));

            // When
            Map<String, Object> result = helper.postWithDefault("/api/create", new HashMap<>(), defaultValue);

            // Then
            assertThat(result).isEqualTo(defaultValue);
        }
    }

    @Nested
    @DisplayName("PUT 요청 테스트")
    class PutRequestTests {

        @Test
        @DisplayName("PUT 요청 성공")
        void put_Success() {
            // Given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", "updated");

            Map<String, Object> expectedResponse = new HashMap<>();
            expectedResponse.put("updated", true);

            when(tradingApiRestTemplate.exchange(
                    eq("/api/update/1"),
                    eq(HttpMethod.PUT),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            // When
            Map<String, Object> result = helper.put("/api/update/1", requestBody, ErrorCode.INTERNAL_ERROR);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("updated")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("DELETE 요청 테스트")
    class DeleteRequestTests {

        @Test
        @DisplayName("DELETE 요청 성공")
        void delete_Success() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    eq("/api/delete/1"),
                    eq(HttpMethod.DELETE),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            // When & Then - 예외 없이 완료되어야 함
            helper.delete("/api/delete/1", ErrorCode.INTERNAL_ERROR);

            verify(tradingApiRestTemplate, times(1)).exchange(
                    eq("/api/delete/1"),
                    eq(HttpMethod.DELETE),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
        }
    }

    // ==================== 에러 변환 테스트 ====================

    @Nested
    @DisplayName("에러 변환 테스트")
    class ErrorTranslationTests {

        @Test
        @DisplayName("404 에러 - NOT_FOUND로 변환")
        void error404_TranslatesToNotFound() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/notfound", ErrorCode.ACCOUNT_NOT_FOUND))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("400 에러 - BAD_REQUEST로 변환")
        void error400_TranslatesToBadRequest() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/bad", ErrorCode.BAD_REQUEST))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("401 에러 - UNAUTHORIZED로 변환")
        void error401_TranslatesToUnauthorized() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/auth", ErrorCode.UNAUTHORIZED))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("500 에러 - INTERNAL_ERROR로 변환")
        void error500_TranslatesToInternalError() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/error", ErrorCode.INTERNAL_ERROR))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    });
        }

        @Test
        @DisplayName("연결 실패 - CONNECTION_FAILED로 변환")
        void connectionFailure_TranslatesToConnectionFailed() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Connection refused", new ConnectException("Connection refused")));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/connect", ErrorCode.CONNECTION_FAILED))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONNECTION_FAILED);
                    });
        }
    }

    // ==================== Retry 동작 테스트 ====================

    @Nested
    @DisplayName("Retry 동작 테스트")
    class RetryBehaviorTests {

        @Test
        @DisplayName("첫 번째 시도 실패 시 재시도 후 성공")
        void retry_FirstFailureThenSuccess() {
            // Given
            Map<String, Object> expectedResponse = new HashMap<>();
            expectedResponse.put("status", "UP");

            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            ))
            .thenThrow(new ResourceAccessException("Temporary failure"))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            // When
            Map<String, Object> result = helper.get("/health", ErrorCode.CONNECTION_FAILED);

            // Then - 2번 호출됨 (1회 실패 + 1회 성공)
            verify(tradingApiRestTemplate, times(2)).exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
            assertThat(result.get("status")).isEqualTo("UP");
        }

        @Test
        @DisplayName("최대 재시도 횟수 초과 시 예외 발생")
        void retry_MaxAttemptsExceeded_ThrowsException() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Persistent failure"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/health", ErrorCode.CONNECTION_FAILED))
                    .isInstanceOf(TradingApiException.class);

            // 2번 시도됨 (maxAttempts = 2)
            verify(tradingApiRestTemplate, times(2)).exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
        }
    }

    // ==================== URL 빌더 테스트 ====================

    @Nested
    @DisplayName("URL 빌더 테스트")
    class UrlBuilderTests {

        @Test
        @DisplayName("파라미터 없이 URL 빌드")
        void buildUrl_NoParams() {
            // When
            String url = helper.buildUrl("/api/test", null);

            // Then
            assertThat(url).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("파라미터와 함께 URL 빌드")
        void buildUrl_WithParams() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("page", "1");
            params.put("size", "10");

            // When
            String url = helper.buildUrl("/api/test", params);

            // Then
            assertThat(url).contains("/api/test?");
            assertThat(url).contains("page=1");
            assertThat(url).contains("size=10");
        }

        @Test
        @DisplayName("빈 파라미터 값은 무시")
        void buildUrl_IgnoresEmptyParams() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("page", "1");
            params.put("filter", "");
            params.put("sort", null);

            // When
            String url = helper.buildUrl("/api/test", params);

            // Then
            assertThat(url).contains("page=1");
            assertThat(url).doesNotContain("filter");
            assertThat(url).doesNotContain("sort");
        }
    }

    // ==================== 유틸리티 메서드 테스트 ====================

    @Nested
    @DisplayName("유틸리티 메서드 테스트")
    class UtilityMethodTests {

        @Test
        @DisplayName("빈 Map 생성")
        void emptyMap_ReturnsEmptyHashMap() {
            // When
            Map<String, Object> result = helper.emptyMap();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("에러 응답 생성")
        void createErrorResponse_CreatesErrorMap() {
            // When
            Map<String, Object> result = helper.createErrorResponse("Test error");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("error")).isEqualTo("Test error");
            assertThat(result.get("success")).isEqualTo(false);
        }
    }
}
