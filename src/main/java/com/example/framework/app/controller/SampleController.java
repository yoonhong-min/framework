package com.example.framework.app.controller;

import com.example.framework.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record SignupRequest(
        @NotBlank @Size(min = 2, max = 20) String name,
        @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password
) {}

@RestController
@RequestMapping("/api/public")
public class SampleController {

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        // do something
        return ResponseEntity.ok(ApiResponse.ok("signed"));
    }
}