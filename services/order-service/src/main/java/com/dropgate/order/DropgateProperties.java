package com.dropgate.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "dropgate")
public class DropgateProperties {

    @NotNull
    private Long holdTtlSeconds = 30L;

    @Min(1)
    private int maxPerAccount = 5;

    public Long getHoldTtlSeconds() {
        return holdTtlSeconds;
    }

    public void setHoldTtlSeconds(Long holdTtlSeconds) {
        this.holdTtlSeconds = holdTtlSeconds;
    }

    public int getMaxPerAccount() {
        return maxPerAccount;
    }

    public void setMaxPerAccount(int maxPerAccount) {
        this.maxPerAccount = maxPerAccount;
    }
}
