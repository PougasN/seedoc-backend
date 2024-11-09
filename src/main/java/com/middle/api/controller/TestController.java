package com.middle.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<String> publicAccess() {
        return ResponseEntity.ok("Public Content - Anyone can access");
    }

    @GetMapping("/user")
    public ResponseEntity<String> userAccess(Authentication authentication) {
        return ResponseEntity.ok("User Content - Logged in as: " + authentication.getName()
                + " with roles: " + authentication.getAuthorities());
    }

    @GetMapping("/admin")
    public ResponseEntity<String> adminAccess(Authentication authentication) {
        return ResponseEntity.ok("Admin Content - Logged in as: " + authentication.getName()
                + " with roles: " + authentication.getAuthorities());
    }
}