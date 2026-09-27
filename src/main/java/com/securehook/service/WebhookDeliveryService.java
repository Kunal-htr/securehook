package com.securehook.service;

import com.securehook.model.DeliveryAttempt;
import com.securehook.model.Subscription;
import com.securehook.repository.DeliveryAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private final RestClient restClient;
    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public WebhookDeliveryService(DeliveryAttemptRepository deliveryAttemptRepository) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        
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

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setEventId(eventId);
        attempt.setSubscriptionId(subscription.getId());
        attempt.setAttemptNumber(1);

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(subscription.getTargetUrl())
                    // No X-Signature header here yet
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            attempt.setHttpStatus(response.getStatusCode().value());
            attempt.setSuccess(response.getStatusCode().is2xxSuccessful());
            attempt.setResponseBody(truncate(response.getBody(), 2000));

            logger.info("Successfully delivered event {} to {}. Status Code: {}", 
                    eventId, subscription.getTargetUrl(), response.getStatusCode().value());

        } catch (RestClientResponseException e) {
            // Handles 4xx and 5xx HTTP responses
            attempt.setHttpStatus(e.getStatusCode().value());
            attempt.setSuccess(false);
            attempt.setResponseBody(truncate(e.getResponseBodyAsString(), 2000));
            
            logger.error("HTTP error delivering event {} to {}. Status: {}", 
                    eventId, subscription.getTargetUrl(), e.getStatusCode().value());

        } catch (RestClientException e) {
            // Handles connection timeouts, DNS errors, etc. (No HTTP response)
            attempt.setHttpStatus(null);
            attempt.setSuccess(false);
            attempt.setResponseBody(truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 2000));

            logger.error("Connection error delivering event {} to {}. Error: {}", 
                    eventId, subscription.getTargetUrl(), e.getMessage());
        }

        deliveryAttemptRepository.save(attempt);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
