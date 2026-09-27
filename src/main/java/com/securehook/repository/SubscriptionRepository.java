package com.securehook.repository;

import com.securehook.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("SELECT s FROM Subscription s JOIN s.eventTypes e WHERE s.active = true AND e = :eventType")
    List<Subscription> findActiveSubscriptionsByEventType(@Param("eventType") String eventType);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"eventTypes"})
    List<Subscription> findAll();
}
