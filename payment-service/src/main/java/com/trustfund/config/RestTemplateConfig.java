package com.trustfund.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * Default client for payment / identity / campaign mutations (moderate timeouts).
     */
    @Bean
    @Primary
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Short timeouts for optional enrichment (e.g. campaign title on my-donations).
     * If campaign-service is slow or unreachable, we fail fast and still return donation rows.
     */
    @Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    public RestTemplate campaignEnrichmentRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }
}
