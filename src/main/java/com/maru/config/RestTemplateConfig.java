package com.maru.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Configuration for Trading System API Integration
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate tradingApiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri("http://localhost:8099")  // Trading System API base URL
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
