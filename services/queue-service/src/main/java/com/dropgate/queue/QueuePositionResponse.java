package com.dropgate.queue;

public record QueuePositionResponse(String dropId, String accountId, String ticketId, long queuePosition) {
}
