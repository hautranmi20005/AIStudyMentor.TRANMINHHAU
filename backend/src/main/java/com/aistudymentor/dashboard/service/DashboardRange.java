package com.aistudymentor.dashboard.service;

import com.aistudymentor.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum DashboardRange {
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30),
    ALL("all", 0);

    private final String value;
    private final int days;

    DashboardRange(String value, int days) {
        this.value = value;
        this.days = days;
    }

    public String value() {
        return value;
    }

    public int days() {
        return days;
    }

    public static DashboardRange parse(String value) {
        for (DashboardRange range : values()) {
            if (range.value.equalsIgnoreCase(value)) {
                return range;
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DASHBOARD_RANGE",
                "range must be one of: 7d, 30d, all.");
    }
}
