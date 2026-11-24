package com.aistudymentor.account.repository;

import com.aistudymentor.account.entity.AccountDeletionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountDeletionJobRepository extends JpaRepository<AccountDeletionJob, String> {
    List<AccountDeletionJob> findTop50ByOrderByCreatedAtAsc();
}
