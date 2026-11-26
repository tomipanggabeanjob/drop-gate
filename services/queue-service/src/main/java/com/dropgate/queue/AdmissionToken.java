package com.dropgate.queue;

import java.io.Serializable;
import java.time.Instant;

public record AdmissionToken(String dropId, String accountId, String token, Instant expiresAt) implements Serializable {
}
