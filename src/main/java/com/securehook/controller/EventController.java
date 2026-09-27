package com.securehook.controller;

import com.securehook.dto.EventPublishRequest;
import com.securehook.dto.EventPublishResponse;
import com.securehook.model.Subscription;
import com.securehook.repository.SubscriptionRepository;
import com.securehook.service.WebhookDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class EventController {

    private final SubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryService webhookDeliveryService;

    public EventController(SubscriptionRepository subscriptionRepository, WebhookDeliveryService webhookDeliveryService) {
        this.subscriptionRepository = subscriptionRepository;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @PostMapping
    public ResponseEntity<EventPublishResponse> publishEvent(@Valid @RequestBody EventPublishRequest request) {
        List<Subscription> matchedSubscriptions = subscriptionRepository.findActiveSubscriptionsByEventType(request.getEventType());

        UUID eventId = UUID.randomUUID();

        List<EventPublishResponse.MatchedSubscriber> matchedSubscribers = matchedSubscriptions.stream()
                .map(sub -> new EventPublishResponse.MatchedSubscriber(sub.getId(), sub.getTargetUrl()))
                .collect(Collectors.toList());

        // Dispatch async delivery tasks
        for (Subscription sub : matchedSubscriptions) {
            webhookDeliveryService.deliverEvent(sub, eventId, request.getEventType(), request.getPayload());
        }

        EventPublishResponse response = new EventPublishResponse();
        response.setEventId(eventId);
        response.setEventType(request.getEventType());
        response.setMatchedSubscribers(matchedSubscribers);
        response.setMatchedSubscriberCount(matchedSubscribers.size());

        return ResponseEntity.ok(response);
    }
}
