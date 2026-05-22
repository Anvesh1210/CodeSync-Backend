package com.codesync.auth.controller;

import com.codesync.auth.dto.request.PremiumUpdateRequest;
import com.codesync.auth.dto.response.UserResponse;
import com.codesync.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/premium")
@RequiredArgsConstructor
public class PremiumUpdateController {

    private final AuthService authService;

    @PostMapping("/update")
    public ResponseEntity<UserResponse> updatePremiumStatus(@RequestBody PremiumUpdateRequest request) {
        return ResponseEntity.ok(authService.updatePremiumStatus(request));
    }
}
