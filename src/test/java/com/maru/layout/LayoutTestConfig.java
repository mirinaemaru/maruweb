package com.maru.layout;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/**
 * Test configuration for layout template tests.
 * Enables Thymeleaf for actual template rendering while excluding
 * JPA and Security auto-configuration.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
public class LayoutTestConfig {
}
