package com.aistudymentor.auth.repository;

import com.aistudymentor.auth.entity.MfaAttempt;
import com.aistudymentor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface MfaAttemptRepository extends JpaRepository<MfaAttempt, UUID> {

    @Query("SELECT COUNT(a) FROM MfaAttempt a WHERE a.user = :user AND a.success = false AND a.createdAt >= :since")
    long countFailedAttemptsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM MfaAttempt a WHERE a.ip = :ip AND a.success = false AND a.createdAt >= :since")
    long countFailedAttemptsByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
