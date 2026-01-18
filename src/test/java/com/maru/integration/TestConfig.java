package com.maru.integration;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 통합테스트용 DataSource 설정
 * P6Spy 데코레이터 문제를 우회하기 위해 명시적으로 H2 DataSource를 생성
 */
@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL")
                .username("sa")
                .password("")
                .build();
    }
}
