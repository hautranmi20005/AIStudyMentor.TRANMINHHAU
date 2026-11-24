package com.aistudymentor.auth.controller;

import com.aistudymentor.auth.dto.PasswordRecoveryRequest;
import com.aistudymentor.auth.dto.PasswordResetRequest;
import com.aistudymentor.auth.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/forgot")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordRecoveryRequest request) {
        passwordRecoveryService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
