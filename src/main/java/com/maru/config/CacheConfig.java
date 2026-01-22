package com.maru.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 캐싱 설정
 * - Trading API 응답을 캐싱하여 성능 개선
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 이름 상수
     */
    public static final String CACHE_INSTRUMENTS = "instruments";
    public static final String CACHE_ACCOUNTS = "accounts";
    public static final String CACHE_STRATEGIES = "strategies";
    public static final String CACHE_HEALTH = "healthStatus";

    /**
     * 기본 캐시 매니저 (TTL: 5분)
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }

    /**
     * 짧은 TTL 캐시 매니저 (TTL: 30초) - 자주 변하는 데이터용
     */
    @Bean("shortTtlCacheManager")
    public CacheManager shortTtlCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats());
        return cacheManager;
    }

    /**
     * 긴 TTL 캐시 매니저 (TTL: 30분) - 거의 변하지 않는 데이터용
     */
    @Bean("longTtlCacheManager")
    public CacheManager longTtlCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats());
        return cacheManager;
    }
}
