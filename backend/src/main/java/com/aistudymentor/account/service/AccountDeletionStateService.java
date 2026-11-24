package com.aistudymentor.account.service;

import com.aistudymentor.account.repository.AccountDeletionAuditRepository;
import com.aistudymentor.account.repository.AccountDeletionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDeletionStateService {

    private final AccountDeletionJobRepository deletionJobRepository;
    private final AccountDeletionAuditRepository deletionAuditRepository;

    public boolean isDeletionPending(UUID userId) {
        return deletionJobRepository.existsById(userId.toString());
    }

    public boolean isDeleted(UUID userId) {
        return deletionAuditRepository.existsById(AccountPrivacyService.hashUserId(userId));
    }
}
