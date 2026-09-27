package com.securehook.model;

public enum EventType {
    ORDER_CREATED("order.created"),
    ORDER_PLACED("order.placed"),
    PAYMENT_FAILED("payment.failed");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String type) {
        for (EventType eventType : values()) {
            if (eventType.value.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
