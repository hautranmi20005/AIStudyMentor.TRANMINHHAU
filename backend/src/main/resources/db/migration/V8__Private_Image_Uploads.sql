ALTER TABLE question_attachments
    ADD COLUMN size_bytes BIGINT NULL,
    ADD COLUMN sha256 VARCHAR(64) NULL;

CREATE TABLE image_uploads (
    upload_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    attached_at TIMESTAMP(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_image_uploads PRIMARY KEY (upload_id),
    CONSTRAINT uq_image_uploads_storage_path UNIQUE (storage_path),
    CONSTRAINT fk_image_uploads_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_image_uploads_profile_created ON image_uploads (profile_id, created_at);
CREATE INDEX idx_image_uploads_cleanup ON image_uploads (created_at, attached_at);
