package com.securehook.service;

import com.securehook.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private final RestClient restClient;

    public WebhookDeliveryService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(5000);    // 5 seconds

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Async("deliveryExecutor")
    public void deliverEvent(Subscription subscription, UUID eventId, String eventType, Map<String, Object> payload) {
        // NOTE: HMAC-SHA256 signature generation is intentionally omitted here 
        // as per the requirement constraints (scoping for Module 3.2).
        
        logger.info("Attempting to deliver event {} ({}) to {}", eventId, eventType, subscription.getTargetUrl());

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(subscription.getTargetUrl())
                    // No X-Signature header here yet
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Successfully delivered event {} to {}. Status Code: {}", 
                    eventId, subscription.getTargetUrl(), response.getStatusCode().value());

        } catch (RestClientException e) {
            logger.error("Failed to deliver event {} to {}. Error: {}", 
                    eventId, subscription.getTargetUrl(), e.getMessage());
        }
    }
}
