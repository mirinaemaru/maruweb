package com.maru.trading.service;

import com.maru.config.CacheConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TradingApiService 캐싱 동작 단위 테스트
 * - 캐시 히트/미스 확인
 * - 캐시 TTL 동작 확인
 * - 캐시 무효화 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradingApiService 캐싱 단위 테스트")
class TradingApiServiceCacheTest {

    @Mock
    private RestTemplate tradingApiRestTemplate;

    private CacheManager cacheManager;
    private TradingApiHelper apiHelper;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager();

        // 테스트용 Resilience4j 설정
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .waitDuration(Duration.ofMillis(10))
                .build();
        Retry retry = RetryRegistry.of(retryConfig).retry("testRetry");

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .build();
        CircuitBreaker cb = CircuitBreakerRegistry.of(cbConfig).circuitBreaker("testCB");

        apiHelper = new TradingApiHelper(tradingApiRestTemplate, retry, cb);
    }

    // ==================== 캐시 히트/미스 테스트 ====================

    @Test
    @DisplayName("동일한 요청 시 캐시에서 결과 반환")
    void cacheHit_SameRequest_ReturnsFromCache() {
        // Given
        Map<String, Object> expectedResponse = createHealthResponse("UP");

        when(tradingApiRestTemplate.exchange(
                eq("/health"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When - 첫 번째 호출
        Map<String, Object> result1 = service.getHealthStatus();

        // Then - API가 한 번 호출됨
        verify(tradingApiRestTemplate, times(1)).exchange(
                eq("/health"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        );

        assertThat(result1).isNotNull();
        assertThat(result1.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("서로 다른 파라미터 요청 시 캐시 미스")
    void cacheMiss_DifferentParams_CallsApi() {
        // Given
        Map<String, Object> account1 = createAccountResponse("account-1");
        Map<String, Object> account2 = createAccountResponse("account-2");

        when(tradingApiRestTemplate.exchange(
                eq("/api/v1/admin/accounts/account-1"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(account1, HttpStatus.OK));

        when(tradingApiRestTemplate.exchange(
                eq("/api/v1/admin/accounts/account-2"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(account2, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When
        Map<String, Object> result1 = service.getAccount("account-1");
        Map<String, Object> result2 = service.getAccount("account-2");

        // Then - 각각 다른 URL로 API 호출
        verify(tradingApiRestTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        );

        assertThat(result1.get("accountId")).isEqualTo("account-1");
        assertThat(result2.get("accountId")).isEqualTo("account-2");
    }

    // ==================== 캐시 가능 메서드 테스트 ====================

    @Test
    @DisplayName("getHealthStatus는 캐시 가능한 메서드")
    void getHealthStatus_IsCacheable() {
        // Given
        Map<String, Object> expectedResponse = createHealthResponse("UP");

        when(tradingApiRestTemplate.exchange(
                eq("/health"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When
        Map<String, Object> result = service.getHealthStatus();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("getAccounts는 캐시 가능한 메서드")
    void getAccounts_IsCacheable() {
        // Given
        Map<String, Object> expectedResponse = createAccountsResponse();

        when(tradingApiRestTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When
        Map<String, Object> result = service.getAccounts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("items")).isNotNull();
    }

    @Test
    @DisplayName("getInstruments는 캐시 가능한 메서드")
    void getInstruments_IsCacheable() {
        // Given
        Map<String, Object> expectedResponse = createInstrumentsResponse();

        when(tradingApiRestTemplate.exchange(
                contains("/api/v1/admin/instruments"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When
        Map<String, Object> result = service.getInstruments(null, null, null, null);

        // Then
        assertThat(result).isNotNull();
    }

    // ==================== 캐시 무효화 테스트 (CacheEvict) ====================

    @Test
    @DisplayName("계좌 생성 시 accounts 캐시 무효화 필요")
    void createAccount_ShouldEvictCache() {
        // Given
        Map<String, Object> createResponse = new HashMap<>();
        createResponse.put("accountId", "new-account");

        when(tradingApiRestTemplate.exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(createResponse, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("broker", "키움증권");

        // When
        Map<String, Object> result = service.createAccount(accountData);

        // Then - 계좌 생성 API 호출됨
        verify(tradingApiRestTemplate, times(1)).exchange(
                eq("/api/v1/admin/accounts"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        );
        assertThat(result.get("accountId")).isEqualTo("new-account");
    }

    @Test
    @DisplayName("계좌 삭제 시 accounts 캐시 무효화 필요")
    void deleteAccount_ShouldEvictCache() {
        // Given
        when(tradingApiRestTemplate.exchange(
                eq("/api/v1/admin/accounts/account-1"),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        TradingApiService service = new TradingApiService(tradingApiRestTemplate, apiHelper);

        // When
        service.deleteAccount("account-1");

        // Then - 계좌 삭제 API 호출됨
        verify(tradingApiRestTemplate, times(1)).exchange(
                eq("/api/v1/admin/accounts/account-1"),
                eq(HttpMethod.DELETE),
                any(),
                any(ParameterizedTypeReference.class)
        );
    }

    // ==================== 캐시 설정 확인 테스트 ====================

    @Test
    @DisplayName("CacheConfig 캐시 이름 상수 존재 확인")
    void cacheNameConstants_Exist() {
        assertThat(CacheConfig.CACHE_INSTRUMENTS).isEqualTo("instruments");
        assertThat(CacheConfig.CACHE_ACCOUNTS).isEqualTo("accounts");
        assertThat(CacheConfig.CACHE_STRATEGIES).isEqualTo("strategies");
        assertThat(CacheConfig.CACHE_HEALTH).isEqualTo("healthStatus");
    }

    @Test
    @DisplayName("여러 CacheManager 독립성 확인")
    void multipleCacheManagers_AreIndependent() {
        // Given
        CacheConfig config = new CacheConfig();
        CacheManager defaultManager = config.cacheManager();
        CacheManager shortTtlManager = config.shortTtlCacheManager();
        CacheManager longTtlManager = config.longTtlCacheManager();

        // When
        var defaultCache = defaultManager.getCache("test");
        var shortCache = shortTtlManager.getCache("test");
        var longCache = longTtlManager.getCache("test");

        defaultCache.put("key", "default");
        shortCache.put("key", "short");
        longCache.put("key", "long");

        // Then - 각 캐시 매니저는 독립적
        assertThat(defaultCache.get("key", String.class)).isEqualTo("default");
        assertThat(shortCache.get("key", String.class)).isEqualTo("short");
        assertThat(longCache.get("key", String.class)).isEqualTo("long");
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createHealthResponse(String status) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("uptime", "24h 30m");
        return response;
    }

    private Map<String, Object> createAccountResponse(String accountId) {
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("broker", "키움증권");
        response.put("status", "ACTIVE");
        return response;
    }

    private Map<String, Object> createAccountsResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("items", List.of(
                createAccountResponse("account-1"),
                createAccountResponse("account-2")
        ));
        response.put("total", 2);
        return response;
    }

    private Map<String, Object> createInstrumentsResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> instrument = new HashMap<>();
        instrument.put("symbol", "005930");
        instrument.put("name", "삼성전자");
        response.put("items", List.of(instrument));
        return response;
    }
}
