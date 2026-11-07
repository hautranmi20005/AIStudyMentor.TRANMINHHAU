package com.aistudymentor.profile.repository;

import com.aistudymentor.auth.entity.User;
import com.aistudymentor.profile.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUser(User user);
    Optional<StudentProfile> findByUser_UserId(UUID userId);
}
