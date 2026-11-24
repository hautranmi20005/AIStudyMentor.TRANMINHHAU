package com.aistudymentor.account.controller;

import com.aistudymentor.account.dto.AccountReauthenticationRequest;
import com.aistudymentor.account.service.AccountPrivacyService;
import com.aistudymentor.config.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountPrivacyService accountPrivacyService;

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) @Valid AccountReauthenticationRequest request,
            HttpServletRequest httpRequest) {
        accountPrivacyService.deleteAccount(principal.getUserId(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountReauthenticationRequest request,
            HttpServletRequest httpRequest) {
        accountPrivacyService.logoutAllDevices(principal.getUserId(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
