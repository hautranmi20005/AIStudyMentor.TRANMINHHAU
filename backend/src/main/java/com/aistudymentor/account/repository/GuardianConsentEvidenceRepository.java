package com.aistudymentor.account.repository;

import com.aistudymentor.account.entity.GuardianConsentEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface GuardianConsentEvidenceRepository extends JpaRepository<GuardianConsentEvidence, String> {
    @Modifying
    @Query("delete from GuardianConsentEvidence evidence "
            + "where evidence.expiresAt is not null and evidence.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
