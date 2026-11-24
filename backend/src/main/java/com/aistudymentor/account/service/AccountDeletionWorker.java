package com.aistudymentor.account.service;

import com.aistudymentor.account.repository.AccountDeletionJobRepository;
import com.aistudymentor.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDeletionWorker {

    private final AccountDeletionJobRepository deletionJobRepository;
    private final AccountPrivacyService accountPrivacyService;

    @Scheduled(fixedDelayString = "${app.privacy.deletion-retry-interval-ms:60000}")
    public void resumePendingDeletions() {
        deletionJobRepository.findTop50ByOrderByCreatedAtAsc().forEach(job -> {
            try {
                accountPrivacyService.resumeDeletion(java.util.UUID.fromString(job.getUserId()));
            } catch (BusinessException | IllegalStateException | IllegalArgumentException exception) {
                log.warn("Account deletion remains pending ({})", exception.getClass().getSimpleName());
            }
        });
    }
}
