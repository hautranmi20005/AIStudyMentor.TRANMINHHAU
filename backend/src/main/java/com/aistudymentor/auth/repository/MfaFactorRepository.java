package com.aistudymentor.auth.repository;

import com.aistudymentor.auth.entity.MfaFactor;
import com.aistudymentor.auth.entity.MfaType;
import com.aistudymentor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaFactorRepository extends JpaRepository<MfaFactor, UUID> {
    List<MfaFactor> findByUser(User user);
    Optional<MfaFactor> findByUserAndType(User user, MfaType type);
}
