package com.aistudymentor.auth.repository;

import com.aistudymentor.auth.entity.MfaRecoveryCode;
import com.aistudymentor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    List<MfaRecoveryCode> findByUser(User user);
    void deleteByUser(User user);
}
