package com.aistudymentor.account.repository;

import com.aistudymentor.account.entity.AccountDeletionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDeletionAuditRepository extends JpaRepository<AccountDeletionAudit, String> {
}
