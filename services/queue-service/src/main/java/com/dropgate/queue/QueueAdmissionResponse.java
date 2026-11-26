package com.dropgate.queue;

public record QueueAdmissionResponse(String dropId, String accountId, String token, long queuePosition) {
}
