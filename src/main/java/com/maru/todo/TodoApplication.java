package com.maru.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@ComponentScan(basePackages = "com.maru")
@EntityScan(basePackages = "com.maru")
@EnableJpaRepositories(basePackages = "com.maru")
public class TodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
