package com.dropgate.queue;

public record QueueAdmissionRequest(String dropId, String accountId, String fingerprint) {
    public QueueAdmissionRequest(String dropId, String accountId) {
        this(dropId, accountId, null);
    }
} 
