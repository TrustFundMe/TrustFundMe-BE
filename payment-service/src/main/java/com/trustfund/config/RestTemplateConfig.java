package com.trustfund.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * Default client for payment / identity / campaign mutations (moderate timeouts).
     * Does NOT use @LoadBalanced - resolves URLs directly without service discovery.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return new RestTemplate(factory);
    }

    /**
     * Short timeouts for optional enrichment (e.g. campaign title on my-donations).
     * If campaign-service is slow or unreachable, we fail fast and still return donation rows.
     * Does NOT use @LoadBalanced - resolves URLs directly without service discovery.
     */
    @Bean
    public RestTemplate campaignEnrichmentRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        return new RestTemplate(factory);
    }
}
