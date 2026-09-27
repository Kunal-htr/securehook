package com.securehook.controller;

import com.securehook.dto.SubscriptionCreateRequest;
import com.securehook.dto.SubscriptionCreateResponse;
import com.securehook.dto.SubscriptionResponse;
import com.securehook.model.Subscription;
import com.securehook.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionCreateResponse> createSubscription(@Valid @RequestBody SubscriptionCreateRequest request) {
        Subscription subscription = subscriptionService.createSubscription(request.getTargetUrl(), request.getEventTypes());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCreateResponse(subscription));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getAllSubscriptions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID id) {
        boolean deleted = subscriptionService.deleteSubscription(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setTargetUrl(subscription.getTargetUrl());
        response.setActive(subscription.isActive());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setEventTypes(subscription.getEventTypes());
        return response;
    }

    private SubscriptionCreateResponse mapToCreateResponse(Subscription subscription) {
        SubscriptionCreateResponse response = new SubscriptionCreateResponse();
        response.setId(subscription.getId());
        response.setTargetUrl(subscription.getTargetUrl());
        response.setActive(subscription.isActive());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setEventTypes(subscription.getEventTypes());
        response.setSecretKey(subscription.getSecretKey());
        return response;
    }
}
