package com.maru.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestTemplateConfig 단위 테스트
 */
@DisplayName("RestTemplateConfig 테스트")
class RestTemplateConfigTest {

    private RestTemplateConfig config;

    @BeforeEach
    void setUp() {
        config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "tradingApiBaseUrl", "http://localhost:8099");
    }

    @Test
    @DisplayName("HttpClient Bean 생성 확인")
    void httpClientBeanCreated() {
        HttpClient httpClient = config.tradingApiHttpClient();
        assertThat(httpClient).isNotNull();
    }

    @Test
    @DisplayName("RestTemplate Bean 생성 확인")
    void restTemplateBeanCreated() {
        HttpClient httpClient = config.tradingApiHttpClient();
        RestTemplate restTemplate = config.tradingApiRestTemplate(httpClient);
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("RetryRegistry Bean 생성 확인")
    void retryRegistryBeanCreated() {
        RetryRegistry retryRegistry = config.tradingApiRetryRegistry();
        assertThat(retryRegistry).isNotNull();
    }

    @Test
    @DisplayName("Retry Bean 생성 및 설정 확인")
    void retryBeanConfigured() {
        RetryRegistry retryRegistry = config.tradingApiRetryRegistry();
        Retry retry = config.tradingApiRetry(retryRegistry);

        assertThat(retry).isNotNull();
        assertThat(retry.getName()).isEqualTo("tradingApi");
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("CircuitBreakerRegistry Bean 생성 확인")
    void circuitBreakerRegistryBeanCreated() {
        CircuitBreakerRegistry registry = config.tradingApiCircuitBreakerRegistry();
        assertThat(registry).isNotNull();
    }

    @Test
    @DisplayName("CircuitBreaker Bean 생성 및 설정 확인")
    void circuitBreakerBeanConfigured() {
        CircuitBreakerRegistry registry = config.tradingApiCircuitBreakerRegistry();
        CircuitBreaker circuitBreaker = config.tradingApiCircuitBreaker(registry);

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("tradingApi");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("CircuitBreaker 설정값 확인")
    void circuitBreakerConfiguration() {
        CircuitBreakerRegistry registry = config.tradingApiCircuitBreakerRegistry();
        CircuitBreaker circuitBreaker = config.tradingApiCircuitBreaker(registry);
        var cbConfig = circuitBreaker.getCircuitBreakerConfig();

        assertThat(cbConfig.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(cbConfig.getSlidingWindowSize()).isEqualTo(10);
        assertThat(cbConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(cbConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(cbConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
    }
}
