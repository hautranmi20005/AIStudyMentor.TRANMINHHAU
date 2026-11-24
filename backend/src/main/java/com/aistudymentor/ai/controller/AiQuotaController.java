package com.aistudymentor.ai.controller;

import com.aistudymentor.ai.dto.AiQuotaResponse;
import com.aistudymentor.ai.service.AiQuotaService;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiQuotaController {

    private final AiQuotaService quotaService;
    private final StudentProfileRepository profileRepository;

    @GetMapping("/quota")
    public AiQuotaResponse getQuota(@AuthenticationPrincipal UserPrincipal principal) {
        return quotaService.remaining(profileRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Student profile not found."))
                .getProfileId());
    }
}
