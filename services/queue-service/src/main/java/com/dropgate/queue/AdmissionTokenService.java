package com.dropgate.queue;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

@Service
public class AdmissionTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SECRET = "dropgate-queue-secret";

    public AdmissionToken issue(String dropId, String accountId, long ttlMillis) {
        String tokenValue = createSignedToken(dropId, accountId, ttlMillis);
        return new AdmissionToken(dropId, accountId, tokenValue, Instant.now().plusMillis(ttlMillis));
    }

    public boolean isValid(AdmissionToken token) {
        if (token == null || token.token() == null || token.token().isBlank()) {
            return false;
        }
        return token.expiresAt().isAfter(Instant.now());
    }

    private String createSignedToken(String dropId, String accountId, long ttlMillis) {
        String payload = dropId + ":" + accountId + ":" + Instant.now().plusMillis(ttlMillis).toEpochMilli();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." +
                    Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to sign queue admission token", ex);
        }
    }
}
