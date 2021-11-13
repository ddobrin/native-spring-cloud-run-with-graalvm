package com.example.hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class SpringNativeFunctionApplication {

    @Value("${TARGET:from-function}")
    String target;

    public static void main(String[] args) {
        SpringApplication.run(SpringNativeFunctionApplication.class, args);
    }

    @Bean
    public Function<String, String> hello() {
        return (in) -> {
            return "Hello: " + in + ", Source: " + target;
        };
    }
}
