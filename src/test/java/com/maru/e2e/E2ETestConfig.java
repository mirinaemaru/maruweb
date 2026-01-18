package com.maru.e2e;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * E2E 테스트용 추가 설정
 * 실제 cautostock 서버와 통신하기 위한 RestTemplate 설정
 * DataSource는 TestConfig에서 제공
 */
@Configuration
public class E2ETestConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
