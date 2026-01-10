package com.maru.trading.controller;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * Minimal configuration for @WebMvcTest in trading controller tests.
 * This configuration excludes JPA, Security, and Thymeleaf auto-configuration.
 * Uses a primary ViewResolver that returns mock views to avoid template rendering issues.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ThymeleafAutoConfiguration.class
})
public class TestConfig {

    /**
     * Primary ViewResolver that returns mock views without actual template rendering.
     * This allows controller tests to pass without requiring actual template files
     * or matching all model attributes that templates expect.
     */
    @Bean
    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ViewResolver mockViewResolver() {
        return new ViewResolver() {
            @Override
            public View resolveViewName(String viewName, Locale locale) {
                return new MockView(viewName);
            }
        };
    }

    /**
     * A mock view that does nothing but set status to 200.
     */
    private static class MockView implements View {
        private final String viewName;

        MockView(String viewName) {
            this.viewName = viewName;
        }

        @Override
        public String getContentType() {
            return "text/html";
        }

        @Override
        public void render(java.util.Map<String, ?> model,
                          javax.servlet.http.HttpServletRequest request,
                          javax.servlet.http.HttpServletResponse response) throws Exception {
            // Do nothing - just return empty response with 200 status
            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().write("<!-- Mock view: " + viewName + " -->");
        }
    }
}
