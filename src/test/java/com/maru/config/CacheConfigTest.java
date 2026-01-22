package com.maru.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CacheConfig 단위 테스트
 */
@DisplayName("CacheConfig 테스트")
class CacheConfigTest {

    private CacheConfig cacheConfig;
    private CacheManager defaultCacheManager;
    private CacheManager shortTtlCacheManager;
    private CacheManager longTtlCacheManager;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
        defaultCacheManager = cacheConfig.cacheManager();
        shortTtlCacheManager = cacheConfig.shortTtlCacheManager();
        longTtlCacheManager = cacheConfig.longTtlCacheManager();
    }

    @Test
    @DisplayName("기본 CacheManager 생성 확인")
    void defaultCacheManagerCreated() {
        assertThat(defaultCacheManager).isNotNull();
    }

    @Test
    @DisplayName("Short TTL CacheManager 생성 확인")
    void shortTtlCacheManagerCreated() {
        assertThat(shortTtlCacheManager).isNotNull();
    }

    @Test
    @DisplayName("Long TTL CacheManager 생성 확인")
    void longTtlCacheManagerCreated() {
        assertThat(longTtlCacheManager).isNotNull();
    }

    @Test
    @DisplayName("캐시 저장 및 조회 동작 확인")
    void cacheStoreAndRetrieve() {
        // given
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";

        // when
        var cache = defaultCacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        cache.put(key, value);

        // then
        var cachedValue = cache.get(key, String.class);
        assertThat(cachedValue).isEqualTo(value);
    }

    @Test
    @DisplayName("캐시 삭제 동작 확인")
    void cacheEvict() {
        // given
        String cacheName = "evictTestCache";
        String key = "evictKey";
        String value = "evictValue";

        var cache = defaultCacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();
        cache.put(key, value);

        // when
        cache.evict(key);

        // then
        var cachedValue = cache.get(key, String.class);
        assertThat(cachedValue).isNull();
    }

    @Test
    @DisplayName("캐시 전체 클리어 동작 확인")
    void cacheClear() {
        // given
        String cacheName = "clearTestCache";
        var cache = defaultCacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // when
        cache.clear();

        // then
        assertThat(cache.get("key1", String.class)).isNull();
        assertThat(cache.get("key2", String.class)).isNull();
    }

    @Test
    @DisplayName("서로 다른 CacheManager는 독립적으로 동작")
    void cacheManagersAreIndependent() {
        // given
        String cacheName = "independentCache";
        String key = "independentKey";

        var defaultCache = defaultCacheManager.getCache(cacheName);
        var shortCache = shortTtlCacheManager.getCache(cacheName);
        var longCache = longTtlCacheManager.getCache(cacheName);

        // when
        defaultCache.put(key, "defaultValue");
        shortCache.put(key, "shortValue");
        longCache.put(key, "longValue");

        // then - 각 캐시 매니저는 독립적인 값을 가짐
        assertThat(defaultCache.get(key, String.class)).isEqualTo("defaultValue");
        assertThat(shortCache.get(key, String.class)).isEqualTo("shortValue");
        assertThat(longCache.get(key, String.class)).isEqualTo("longValue");
    }

    @Test
    @DisplayName("캐시 이름 상수 확인")
    void cacheNameConstants() {
        assertThat(CacheConfig.CACHE_INSTRUMENTS).isEqualTo("instruments");
        assertThat(CacheConfig.CACHE_ACCOUNTS).isEqualTo("accounts");
        assertThat(CacheConfig.CACHE_STRATEGIES).isEqualTo("strategies");
        assertThat(CacheConfig.CACHE_HEALTH).isEqualTo("healthStatus");
    }
}
