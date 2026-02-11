package com.maru.habit.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
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
 * Minimal configuration for @WebMvcTest in habit controller tests.
 */
@Configuration("habitControllerTestConfig")
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ThymeleafAutoConfiguration.class
})
public class TestConfig {

    @Bean("habitMockViewResolver")
    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ViewResolver habitMockViewResolver() {
        return new ViewResolver() {
            @Override
            public View resolveViewName(String viewName, Locale locale) {
                return new MockView(viewName);
            }
        };
    }

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
            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().write("<!-- Mock view: " + viewName + " -->");
        }
    }
}
