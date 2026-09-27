package com.securehook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import java.util.Set;
import com.securehook.validation.ValidEventType;

public class SubscriptionCreateRequest {

    @NotBlank
    @URL
    private String targetUrl;

    @NotEmpty
    @ValidEventType
    private Set<String> eventTypes;

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Set<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(Set<String> eventTypes) {
        this.eventTypes = eventTypes;
    }
}
