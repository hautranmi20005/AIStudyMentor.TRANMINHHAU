package com.aistudymentor.notification.entity;

import com.aistudymentor.common.entity.BaseEntity;
import com.aistudymentor.profile.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference extends BaseEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "profile_id", nullable = false, length = 36)
    private UUID profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, insertable = false, updatable = false)
    private StudentProfile profile;

    @Column(name = "bookmark_review_enabled", nullable = false)
    private boolean bookmarkReviewEnabled = true;

    @Column(name = "repeated_mistake_enabled", nullable = false)
    private boolean repeatedMistakeEnabled = true;

    @Column(name = "weekly_summary_enabled", nullable = false)
    private boolean weeklySummaryEnabled = true;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";
}
