package com.sam.notification.model;

public enum Operation {

    CREATE("Account is created", "Your account is created."),
    DELETE("Account is deleted", "Your account is deleted.");

    private final String subject;
    private final String bodyTemplate;

    Operation(String subject, String bodyTemplate) {
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
    }

    public String subject() {
        return subject;
    }

    public String body(String siteName) {
        return bodyTemplate.formatted(siteName);
    }

    public static Operation from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Operation must not be empty");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operation: " + value);
        }
    }
}
