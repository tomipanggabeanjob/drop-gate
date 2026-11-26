package com.dropgate.queue;

public record DropMetadata(String dropId, String name, int stock, long priceCents, boolean exists) {
}
