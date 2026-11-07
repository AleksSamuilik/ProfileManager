package com.sam.profilemanager.event;

public record UserEvent(String operation, String email) {
    public static final String CREATE = "CREATE";
    public static final String DELETE = "DELETE";
}