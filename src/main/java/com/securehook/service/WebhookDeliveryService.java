package com.securehook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securehook.model.DeliveryAttempt;
import com.securehook.model.Subscription;
import com.securehook.repository.DeliveryAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private final RestClient restClient;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final TaskScheduler taskScheduler;
    private final Executor deliveryExecutor;
    private final SignatureService signatureService;
    private final ObjectMapper objectMapper;

    public WebhookDeliveryService(DeliveryAttemptRepository deliveryAttemptRepository,
                                  TaskScheduler taskScheduler,
                                  @Qualifier("deliveryExecutor") Executor deliveryExecutor,
                                  SignatureService signatureService,
                                  ObjectMapper objectMapper) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.taskScheduler = taskScheduler;
        this.deliveryExecutor = deliveryExecutor;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(5000);    // 5 seconds

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Async("deliveryExecutor")
    public void deliverEvent(Subscription subscription, UUID eventId, String eventType, Map<String, Object> payload) {
        executeAttempt(subscription, eventId, eventType, payload, 1);
    }

    private void executeAttempt(Subscription subscription, UUID eventId, String eventType, Map<String, Object> payload, int attemptNumber) {
        logger.info("Attempt {} to deliver event {} ({}) to {}", attemptNumber, eventId, eventType, subscription.getTargetUrl());

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payload for event {}", eventId, e);
            return;
        }

        String signatureHeader = signatureService.generateSignature(jsonPayload, subscription.getSecretKey());

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setEventId(eventId);
        attempt.setSubscriptionId(subscription.getId());
        attempt.setAttemptNumber(attemptNumber);

        boolean success = false;

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(subscription.getTargetUrl())
                    .header("X-Signature", signatureHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .toEntity(String.class);

            attempt.setHttpStatus(response.getStatusCode().value());
            attempt.setSuccess(response.getStatusCode().is2xxSuccessful());
            attempt.setResponseBody(truncate(response.getBody(), 2000));
            success = attempt.isSuccess();

            logger.info("Successfully delivered event {} to {} on attempt {}. Status Code: {}", 
                    eventId, subscription.getTargetUrl(), attemptNumber, response.getStatusCode().value());

        } catch (RestClientResponseException e) {
            attempt.setHttpStatus(e.getStatusCode().value());
            attempt.setSuccess(false);
            attempt.setResponseBody(truncate(e.getResponseBodyAsString(), 2000));
            
            logger.error("HTTP error delivering event {} to {} on attempt {}. Status: {}", 
                    eventId, subscription.getTargetUrl(), attemptNumber, e.getStatusCode().value());

        } catch (RestClientException e) {
            attempt.setHttpStatus(null);
            attempt.setSuccess(false);
            attempt.setResponseBody(truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 2000));

            logger.error("Connection error delivering event {} to {} on attempt {}. Error: {}", 
                    eventId, subscription.getTargetUrl(), attemptNumber, e.getMessage());
        }

        deliveryAttemptRepository.save(attempt);

        if (!success && attemptNumber < 5) {
            long delaySeconds = (long) Math.pow(2, attemptNumber - 1); // 1s, 2s, 4s, 8s
            Instant nextAttemptTime = Instant.now().plus(Duration.ofSeconds(delaySeconds));
            
            logger.info("Scheduling attempt {} for event {} in {} seconds", attemptNumber + 1, eventId, delaySeconds);
            
            taskScheduler.schedule(() -> {
                deliveryExecutor.execute(() -> {
                    executeAttempt(subscription, eventId, eventType, payload, attemptNumber + 1);
                });
            }, nextAttemptTime);
            
        } else if (!success) {
            logger.warn("Exhausted all 5 attempts for event {} to {}", eventId, subscription.getTargetUrl());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
