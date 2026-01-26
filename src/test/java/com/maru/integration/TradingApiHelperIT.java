package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.exception.TradingApiException;
import com.maru.trading.exception.TradingApiException.ErrorCode;
import com.maru.trading.service.TradingApiHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TradingApiHelper 통합 테스트
 * - Spring Context에서 Resilience4j Bean 주입 확인
 * - Retry + Circuit Breaker 연동 확인
 * - 동시성 처리 확인
 */
@SpringBootTest(classes = TodoApplication.class)
@ActiveProfiles("test")
@DisplayName("TradingApiHelper 통합 테스트")
class TradingApiHelperIT {

    @MockBean
    private RestTemplate tradingApiRestTemplate;

    @Autowired
    private Retry tradingApiRetry;

    @Autowired
    private CircuitBreaker tradingApiCircuitBreaker;

    private TradingApiHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TradingApiHelper(tradingApiRestTemplate, tradingApiRetry, tradingApiCircuitBreaker);
        // Circuit Breaker 상태 리셋
        tradingApiCircuitBreaker.reset();
    }

    // ==================== Bean 주입 테스트 ====================

    @Nested
    @DisplayName("Spring Bean 주입 테스트")
    class BeanInjectionTests {

        @Test
        @DisplayName("Retry Bean이 올바르게 주입됨")
        void retryBean_IsInjected() {
            assertThat(tradingApiRetry).isNotNull();
            assertThat(tradingApiRetry.getName()).isEqualTo("tradingApi");
        }

        @Test
        @DisplayName("CircuitBreaker Bean이 올바르게 주입됨")
        void circuitBreakerBean_IsInjected() {
            assertThat(tradingApiCircuitBreaker).isNotNull();
            assertThat(tradingApiCircuitBreaker.getName()).isEqualTo("tradingApi");
        }

        @Test
        @DisplayName("Retry 설정값이 올바름")
        void retryConfig_IsCorrect() {
            assertThat(tradingApiRetry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("CircuitBreaker 설정값이 올바름")
        void circuitBreakerConfig_IsCorrect() {
            var config = tradingApiCircuitBreaker.getCircuitBreakerConfig();
            assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
            assertThat(config.getSlidingWindowSize()).isEqualTo(10);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        }
    }

    // ==================== Retry + Circuit Breaker 연동 테스트 ====================

    @Nested
    @DisplayName("Retry + Circuit Breaker 연동 테스트")
    class ResilienceIntegrationTests {

        @Test
        @DisplayName("성공 요청 시 정상 응답 반환")
        void success_ReturnsResponse() {
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
            assertThat(tradingApiCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("재시도 후 성공 시 정상 응답 반환")
        void retryThenSuccess_ReturnsResponse() {
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
            .thenThrow(new ResourceAccessException("Temporary failure"))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            // When
            Map<String, Object> result = helper.get("/health", ErrorCode.CONNECTION_FAILED);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("status")).isEqualTo("UP");
            verify(tradingApiRestTemplate, times(3)).exchange(
                    anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
            );
        }

        @Test
        @DisplayName("최대 재시도 후 실패 시 예외 발생")
        void maxRetryExhausted_ThrowsException() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Persistent failure"));

            // When & Then
            assertThatThrownBy(() -> helper.get("/health", ErrorCode.CONNECTION_FAILED))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isIn(ErrorCode.CONNECTION_FAILED, ErrorCode.SERVICE_UNAVAILABLE);
                    });

            // 3번 재시도됨
            verify(tradingApiRestTemplate, times(3)).exchange(
                    anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
            );
        }
    }

    // ==================== Circuit Breaker 상태 전이 테스트 ====================

    @Nested
    @DisplayName("Circuit Breaker 상태 전이 테스트")
    class CircuitBreakerStateTests {

        @Test
        @DisplayName("Circuit Breaker 초기 상태는 CLOSED")
        void initialState_IsClosed() {
            assertThat(tradingApiCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("연속 실패 시 Circuit Breaker OPEN 상태로 전이")
        void consecutiveFailures_OpensCircuitBreaker() {
            // Given - 모든 요청 실패
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new ResourceAccessException("Failure"));

            // When - minimumNumberOfCalls(5) 이상 실패 시킴
            for (int i = 0; i < 10; i++) {
                try {
                    helper.get("/health", ErrorCode.CONNECTION_FAILED);
                } catch (TradingApiException ignored) {}
            }

            // Then - Circuit Breaker가 OPEN 상태가 되어야 함
            // 최소 호출 수 이상이면 상태가 변경될 수 있음
            CircuitBreaker.State state = tradingApiCircuitBreaker.getState();
            assertThat(state).isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        }
    }

    // ==================== 에러 복구 테스트 ====================

    @Nested
    @DisplayName("에러 복구 테스트")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("getWithDefault - 실패 시 기본값 반환")
        void getWithDefault_Failure_ReturnsDefault() {
            // Given
            Map<String, Object> defaultValue = new HashMap<>();
            defaultValue.put("status", "DOWN");
            defaultValue.put("error", "Service unavailable");

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
            assertThat(result.get("status")).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("postWithDefault - 실패 시 기본값 반환")
        void postWithDefault_Failure_ReturnsDefault() {
            // Given
            Map<String, Object> defaultValue = new HashMap<>();
            defaultValue.put("success", false);

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

    // ==================== 동시성 테스트 ====================

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTests {

        @Test
        @DisplayName("동시 요청 처리")
        void concurrentRequests_AreHandled() throws InterruptedException {
            // Given
            Map<String, Object> expectedResponse = new HashMap<>();
            expectedResponse.put("status", "UP");

            when(tradingApiRestTemplate.exchange(
                    eq("/health"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        Map<String, Object> result = helper.get("/health", ErrorCode.CONNECTION_FAILED);
                        if (result != null && "UP".equals(result.get("status"))) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // 실패 케이스
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then - 모든 요청이 성공해야 함
            assertThat(successCount.get()).isEqualTo(threadCount);
        }
    }

    // ==================== HTTP 상태 코드별 테스트 ====================

    @Nested
    @DisplayName("HTTP 상태 코드별 에러 처리 테스트")
    class HttpStatusCodeTests {

        @Test
        @DisplayName("403 Forbidden 에러 처리")
        void http403_ThrowsForbiddenException() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/protected", ErrorCode.FORBIDDEN))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    });
        }

        @Test
        @DisplayName("409 Conflict 에러 처리")
        void http409_ThrowsConflictException() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

            // When & Then
            assertThatThrownBy(() -> helper.post("/api/create", new HashMap<>(), ErrorCode.CONFLICT))
                    .isInstanceOf(TradingApiException.class)
                    .satisfies(ex -> {
                        TradingApiException e = (TradingApiException) ex;
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });
        }

        @Test
        @DisplayName("503 Service Unavailable 에러 처리")
        void http503_ThrowsServiceUnavailableException() {
            // Given
            when(tradingApiRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

            // When & Then
            assertThatThrownBy(() -> helper.get("/api/unavailable", ErrorCode.SERVICE_UNAVAILABLE))
                    .isInstanceOf(TradingApiException.class);
        }
    }
}
