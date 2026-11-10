package com.aistudymentor.auth.repository;

import com.aistudymentor.auth.entity.GuardianConsent;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, UUID> {
    Optional<GuardianConsent> findByUser(User user);
    Optional<GuardianConsent> findByUser_UserId(UUID userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select c from GuardianConsent c where c.tokenHash = :tokenHash")
        Optional<GuardianConsent> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select c from GuardianConsent c where c.user.userId = :userId")
        Optional<GuardianConsent> findByUserIdForUpdate(@Param("userId") UUID userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select c from GuardianConsent c where c.status = :status "
            + "and c.lastSentAt <= :sentBefore and c.tokenExpiresAt > :expiresAfter "
            + "and c.resendCount < :maxResend")
        List<GuardianConsent> findPendingReminders(
            @Param("status") ConsentStatus status,
            @Param("sentBefore") LocalDateTime sentBefore,
            @Param("expiresAfter") LocalDateTime expiresAfter,
            @Param("maxResend") int maxResend);
}
