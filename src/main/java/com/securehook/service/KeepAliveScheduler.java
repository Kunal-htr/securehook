package com.securehook.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final String keepAliveUrl;
    private final RestClient restClient;

    public KeepAliveScheduler(@Value("${securehook.keepalive.url:}") String keepAliveUrl) {
        this.keepAliveUrl = keepAliveUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(10000);   // 10 seconds

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @PostConstruct
    public void init() {
        if (keepAliveUrl == null || keepAliveUrl.isBlank()) {
            logger.info("Keep-alive scheduler disabled (URL not set)");
        } else {
            logger.info("Keep-alive scheduler enabled");
        }
    }

    @Scheduled(fixedRateString = "${securehook.keepalive.interval-ms:840000}")
    public void ping() {
        if (keepAliveUrl == null || keepAliveUrl.isBlank()) {
            return;
        }

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(keepAliveUrl)
                    .retrieve()
                    .toEntity(String.class);

            logger.info("Keep-alive ping successful. Status: {}", response.getStatusCode().value());
        } catch (org.springframework.web.client.RestClientResponseException e) {
            logger.warn("Keep-alive ping failed: {} (Status: {})", e.getClass().getSimpleName(), e.getStatusCode().value());
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getClass().getSimpleName());
        }
    }
}
