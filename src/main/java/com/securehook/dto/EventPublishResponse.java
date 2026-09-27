package com.securehook.dto;

import java.util.List;
import java.util.UUID;

public class EventPublishResponse {
    private UUID eventId;
    private String eventType;
    private int matchedSubscriberCount;
    private List<MatchedSubscriber> matchedSubscribers;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getMatchedSubscriberCount() {
        return matchedSubscriberCount;
    }

    public void setMatchedSubscriberCount(int matchedSubscriberCount) {
        this.matchedSubscriberCount = matchedSubscriberCount;
    }

    public List<MatchedSubscriber> getMatchedSubscribers() {
        return matchedSubscribers;
    }

    public void setMatchedSubscribers(List<MatchedSubscriber> matchedSubscribers) {
        this.matchedSubscribers = matchedSubscribers;
    }

    public static class MatchedSubscriber {
        private UUID id;
        private String targetUrl;

        public MatchedSubscriber(UUID id, String targetUrl) {
            this.id = id;
            this.targetUrl = targetUrl;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }
    }
}
