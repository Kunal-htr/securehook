package com.securehook.service;

import com.securehook.model.Subscription;
import com.securehook.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription createSubscription(String targetUrl, Set<String> eventTypes) {
        Subscription subscription = new Subscription();
        subscription.setTargetUrl(targetUrl);
        subscription.setEventTypes(eventTypes);
        subscription.setSecretKey(generateSecretKey());
        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Transactional
    public boolean deleteSubscription(UUID id) {
        if (subscriptionRepository.existsById(id)) {
            subscriptionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private String generateSecretKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
