ALTER TABLE users
    DROP CHECK chk_users_account_status;

ALTER TABLE users
    ADD CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'PENDING_CONSENT', 'SUSPENDED', 'DELETION_PENDING'));
