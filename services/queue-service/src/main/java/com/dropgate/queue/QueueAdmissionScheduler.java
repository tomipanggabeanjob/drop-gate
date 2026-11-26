package com.dropgate.queue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueueAdmissionScheduler {

    private static final int BATCH_SIZE = 10;
    private static final long TOKEN_TTL_MS = 30_000L;

    private final QueueAdmissionService queueAdmissionService;
    private final AdmissionTokenService admissionTokenService;

    public QueueAdmissionScheduler(QueueAdmissionService queueAdmissionService, AdmissionTokenService admissionTokenService) {
        this.queueAdmissionService = queueAdmissionService;
        this.admissionTokenService = admissionTokenService;
    }

    @Scheduled(fixedDelay = 1000)
    public void admitNextBatch() {
        for (String dropId : queueAdmissionService.getActiveDrops()) {
            List<String> queuedAccounts = queueAdmissionService.nextBatch(dropId, BATCH_SIZE);
            for (String accountId : queuedAccounts) {
                AdmissionToken token = admissionTokenService.issue(dropId, accountId, TOKEN_TTL_MS);
                queueAdmissionService.clearAdmission(dropId, accountId);
                queueAdmissionService.issueAdmissionToken(dropId, accountId, TOKEN_TTL_MS);
                queueAdmissionService.pushAdmissionEvent(dropId, accountId, token.token());
            }
        }
    }
}
