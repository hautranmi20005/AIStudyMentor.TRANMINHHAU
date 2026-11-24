package com.aistudymentor.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_audit")
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionAudit {

    @Id
    @Column(name = "user_id_hash", nullable = false, length = 64)
    private String userIdHash;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    public AccountDeletionAudit(String userIdHash, LocalDateTime deletedAt) {
        this.userIdHash = userIdHash;
        this.deletedAt = deletedAt;
    }
}
