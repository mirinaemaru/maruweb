package com.maru.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RestTemplate Configuration for Trading System API Integration
 * - Connection Pool 설정
 * - Resilience4j Retry/Circuit Breaker 설정
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${trading.api.base-url:http://localhost:8099}")
    private String tradingApiBaseUrl;

    // Connection Pool 설정
    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 3000;

    // Retry 설정
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_WAIT_DURATION_MS = 500;

    // Circuit Breaker 설정
    private static final float FAILURE_RATE_THRESHOLD = 50;
    private static final int SLOW_CALL_RATE_THRESHOLD = 100;
    private static final int SLOW_CALL_DURATION_SECONDS = 5;
    private static final int WAIT_DURATION_IN_OPEN_STATE_SECONDS = 60;
    private static final int SLIDING_WINDOW_SIZE = 10;

    /**
     * Connection Pool이 적용된 HttpClient 생성 (HttpClient 4.x)
     */
    @Bean
    public HttpClient tradingApiHttpClient() {
        // Connection Manager with Pool
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // Request Config
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Trading API용 RestTemplate
     */
    @Bean
    public RestTemplate tradingApiRestTemplate(HttpClient tradingApiHttpClient) {
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(tradingApiHttpClient);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // Base URL 설정
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(tradingApiBaseUrl);
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriBuilderFactory);

        log.info("Trading API RestTemplate configured with base URL: {}", tradingApiBaseUrl);
        log.info("Connection Pool: maxTotal={}, maxPerRoute={}", MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE);

        return restTemplate;
    }

    /**
     * Retry Registry - 재시도 정책 정의
     */
    @Bean
    public RetryRegistry tradingApiRetryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_RETRY_ATTEMPTS)
                .waitDuration(Duration.ofMillis(RETRY_WAIT_DURATION_MS))
                .retryExceptions(
                        IOException.class,
                        TimeoutException.class,
                        RestClientException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class
                )
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Trading API용 Retry 인스턴스
     */
    @Bean
    public Retry tradingApiRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry("tradingApi");

        retry.getEventPublisher()
                .onRetry(event -> log.warn("Trading API Retry attempt #{} for {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown"))
                .onError(event -> log.error("Trading API Retry failed after {} attempts",
                        event.getNumberOfRetryAttempts()))
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        log.info("Trading API succeeded after {} retry attempts", event.getNumberOfRetryAttempts());
                    }
                });

        return retry;
    }

    /**
     * Circuit Breaker Registry
     */
    @Bean
    public CircuitBreakerRegistry tradingApiCircuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .slowCallRateThreshold(SLOW_CALL_RATE_THRESHOLD)
                .slowCallDurationThreshold(Duration.ofSeconds(SLOW_CALL_DURATION_SECONDS))
                .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_IN_OPEN_STATE_SECONDS))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Trading API용 Circuit Breaker 인스턴스
     */
    @Bean
    public CircuitBreaker tradingApiCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("tradingApi");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Trading API Circuit Breaker state changed: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> log.warn("Trading API Circuit Breaker is OPEN - call not permitted"))
                .onError(event -> log.error("Trading API Circuit Breaker recorded error: {}",
                        event.getThrowable().getMessage()));

        return circuitBreaker;
    }
}
