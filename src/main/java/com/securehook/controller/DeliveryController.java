package com.securehook.controller;

import com.securehook.dto.DeliveryAttemptResponse;
import com.securehook.model.DeliveryAttempt;
import com.securehook.repository.DeliveryAttemptRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public DeliveryController(DeliveryAttemptRepository deliveryAttemptRepository) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<List<DeliveryAttemptResponse>> getDeliveries(@PathVariable UUID eventId) {
        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByEventIdOrderByAttemptedAtAsc(eventId);

        List<DeliveryAttemptResponse> response = attempts.stream().map(attempt -> {
            DeliveryAttemptResponse dto = new DeliveryAttemptResponse();
            dto.setAttemptNumber(attempt.getAttemptNumber());
            dto.setHttpStatus(attempt.getHttpStatus());
            dto.setSuccess(attempt.isSuccess());
            dto.setResponseBody(attempt.getResponseBody());
            dto.setAttemptedAt(attempt.getAttemptedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
