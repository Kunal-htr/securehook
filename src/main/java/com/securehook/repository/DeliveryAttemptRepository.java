package com.securehook.repository;

import com.securehook.model.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    List<DeliveryAttempt> findByEventIdOrderByAttemptedAtAsc(UUID eventId);
}
