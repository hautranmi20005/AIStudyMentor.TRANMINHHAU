package com.aistudymentor.account.service;

import com.aistudymentor.account.dto.AccountReauthenticationRequest;
import com.aistudymentor.account.entity.AccountDeletionAudit;
import com.aistudymentor.account.entity.AccountDeletionJob;
import com.aistudymentor.account.entity.GuardianConsentEvidence;
import com.aistudymentor.account.repository.AccountDeletionAuditRepository;
import com.aistudymentor.account.repository.AccountDeletionJobRepository;
import com.aistudymentor.account.repository.GuardianConsentEvidenceRepository;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.GuardianConsent;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.GuardianConsentRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.auth.service.AccountStatusService;
import com.aistudymentor.auth.service.AuthService;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.notification.repository.DeviceTokenRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.question.storage.StorageGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountPrivacyService {

    private static final String STORAGE_PATHS_SQL = """
            SELECT storage_path FROM image_uploads WHERE profile_id = ?
            UNION
            SELECT attachment.firebase_storage_path
            FROM question_attachments attachment
            JOIN questions question ON question.question_id = attachment.question_id
            WHERE question.profile_id = ?
            """;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final AccountDeletionJobRepository deletionJobRepository;
    private final AccountDeletionAuditRepository deletionAuditRepository;
    private final GuardianConsentEvidenceRepository consentEvidenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AccountStatusService accountStatusService;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redis;
    private final StorageGateway storage;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.security.rate-limit.key-prefix:studymentor:rate-limit}")
    private String rateLimitKeyPrefix;

    @Value("${app.privacy.guardian-consent-evidence-retention-days:0}")
    private long guardianEvidenceRetentionDays;

    public void deleteAccount(
            UUID userId,
            AccountReauthenticationRequest request,
            String clientIp) {
        String hash = hashUserId(userId);
        if (deletionAuditRepository.existsById(hash)) {
            return;
        }
        if (!deletionJobRepository.existsById(userId.toString())) {
            if (request == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "REAUTHENTICATION_REQUIRED",
                        "A password and multi-factor code are required to delete this account.");
            }
            authService.verifySensitiveAction(userId, request.getPassword(), request.getCode(), clientIp);
            transactionTemplate.executeWithoutResult(status -> beginDeletion(userId, hash));
        }
        resumeDeletion(userId);
    }

    public void resumeDeletion(UUID userId) {
        AccountDeletionJob job = deletionJobRepository.findById(userId.toString())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "No account deletion is pending for this user."));
        deleteStoredImages(userId, parseStoragePaths(job.getStoragePathsJson()));
        purgeUserRedisKeys(userId, UUID.fromString(job.getProfileId()));
        transactionTemplate.executeWithoutResult(
                status -> finalizeDeletion(userId, job.getUserIdHash()));
    }

    @Transactional
    protected void beginDeletion(UUID userId, String userHash) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "The account was not found."));
        if (deletionJobRepository.existsById(userId.toString())) {
            return;
        }
        StudentProfile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "The account profile was not found."));
        List<String> paths = jdbcTemplate.queryForList(
                STORAGE_PATHS_SQL, String.class, profile.getProfileId().toString(), profile.getProfileId().toString());
        AccountDeletionJob job = new AccountDeletionJob();
        job.setUserId(userId.toString());
        job.setProfileId(profile.getProfileId().toString());
        job.setUserIdHash(userHash);
        job.setStoragePathsJson(serializeStoragePaths(new LinkedHashSet<>(paths)));
        deletionJobRepository.save(job);

        user.setAccountStatus(AccountStatus.DELETION_PENDING);
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);
        accountStatusService.invalidateCache(userId);
    }

    @Transactional
    public void logoutAllDevices(
            UUID userId,
            AccountReauthenticationRequest request,
            String clientIp) {
        authService.verifySensitiveAction(userId, request.getPassword(), request.getCode(), clientIp);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "The account was not found."));
        if (user.getAccountStatus() == AccountStatus.DELETION_PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACCOUNT_DELETION_PENDING",
                    "This account is being deleted.");
        }
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);
        profileRepository.findByUser_UserId(userId)
                .ifPresent(profile -> deviceTokenRepository.deleteByProfile_ProfileId(profile.getProfileId()));
    }

    @Transactional
    protected void finalizeDeletion(UUID userId, String userHash) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) {
            if (!deletionAuditRepository.existsById(userHash)) {
                throw new IllegalStateException("Account deletion job lost its account before the audit was recorded.");
            }
            deletionJobRepository.deleteById(userId.toString());
            return;
        }

        GuardianConsent consent = guardianConsentRepository.findByUser_UserId(userId).orElse(null);
        if (consent != null) {
            LocalDateTime expiresAt = guardianEvidenceRetentionDays > 0
                    ? LocalDateTime.now().plusDays(guardianEvidenceRetentionDays)
                    : null;
            consentEvidenceRepository.save(new GuardianConsentEvidence(
                    userHash,
                    consent.getStatus().name(),
                    consent.getVerifiedAt(),
                    expiresAt));
        }

        userRepository.delete(user);
        userRepository.flush();
        deletionAuditRepository.save(new AccountDeletionAudit(userHash, LocalDateTime.now()));
        deletionJobRepository.deleteById(userId.toString());
    }

    public boolean isDeletionPending(UUID userId) {
        return deletionJobRepository.existsById(userId.toString());
    }

    public boolean isDeleted(UUID userId) {
        return deletionAuditRepository.existsById(hashUserId(userId));
    }

    public void cleanupPersistentEphemeralKeys() {
        removePersistentKeys(rateLimitKeyPrefix + ":*");
        removePersistentKeys("studymentor:questions:idempotency:v1:*");
    }

    public static String hashUserId(UUID userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userId.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for account deletion audit.", exception);
        }
    }

    private void deleteStoredImages(UUID userId, List<String> paths) {
        String expectedPrefix = "users/" + userId + "/images/";
        for (String path : paths) {
            if (!path.startsWith(expectedPrefix)) {
                throw new IllegalStateException("Account deletion manifest contains an out-of-scope storage path.");
            }
            try {
                storage.delete(path);
            } catch (IOException exception) {
                log.warn("Account image cleanup is pending.");
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "ACCOUNT_DELETION_PENDING",
                        "Account deletion is queued and will resume when image storage is available.");
            }
        }
    }

    private void purgeUserRedisKeys(UUID userId, UUID profileId) {
        String user = userId.toString();
        String profile = profileId.toString();
        String userHash = sha256(user);
        deleteRedisKey("account:status:" + user);
        deleteRedisKey("user-profile::" + user);
        deleteRedisPattern("studymentor:questions:idempotency:v1:" + user + ":*");
        deleteRedisPattern("studymentor:ai:user:" + profile + ":*");
        deleteRedisPattern("studymentor:ai:quota:" + profile + ":*");
        deleteRedisPattern("studymentor:ai:image-quota:" + profile + ":*");
        deleteRedisPattern(rateLimitKeyPrefix + ":*:" + userHash);
    }

    private void deleteRedisKey(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException exception) {
            throw redisCleanupUnavailable();
        }
    }

    private void deleteRedisPattern(String pattern) {
        try {
            Set<String> keys = scanRedisKeys(pattern);
            List<String> batch = new ArrayList<>(500);
            for (String key : keys) {
                batch.add(key);
                if (batch.size() == 500) {
                    redis.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redis.delete(batch);
            }
        } catch (DataAccessException | IllegalStateException exception) {
            throw redisCleanupUnavailable();
        }
    }

    private void removePersistentKeys(String pattern) {
        try {
            for (String key : scanRedisKeys(pattern)) {
                if (Long.valueOf(-1).equals(redis.getExpire(key, TimeUnit.MILLISECONDS))) {
                    redis.delete(key);
                }
            }
        } catch (DataAccessException | IllegalStateException exception) {
            throw redisCleanupUnavailable();
        }
    }

    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = redis.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> matches = new LinkedHashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(pattern).count(500).build())) {
                while (cursor.hasNext()) {
                    matches.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return matches;
        });
        if (keys == null) {
            throw redisCleanupUnavailable();
        }
        return keys;
    }

    private List<String> parseStoragePaths(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Account deletion storage manifest is unreadable.", exception);
        }
    }

    private String serializeStoragePaths(Set<String> paths) {
        try {
            return objectMapper.writeValueAsString(paths);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Account deletion storage manifest could not be created.", exception);
        }
    }

    private BusinessException redisCleanupUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "ACCOUNT_DELETION_PENDING",
                "Account deletion is queued and will resume when session storage is available.");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for privacy key cleanup.", exception);
        }
    }
}
