package com.example.presentationgenerator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AppConfig {
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Spring Boot 4 uses Jackson 3 by default and auto-configures a JsonMapper
     * when the JSON starter is present. This explicit bean keeps the dependency
     * visible and stable for the Ollama integration.
     */
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
