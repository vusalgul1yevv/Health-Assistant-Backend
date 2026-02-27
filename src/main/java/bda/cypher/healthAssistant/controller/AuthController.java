package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.EmailOtpSendRequestDTO;
import bda.cypher.healthAssistant.dto.EmailOtpSendResponseDTO;
import bda.cypher.healthAssistant.dto.EmailOtpVerifyRequestDTO;
import bda.cypher.healthAssistant.dto.EmailOtpVerifyResponseDTO;
import bda.cypher.healthAssistant.dto.ForgotPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.MessageResponseDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;
import bda.cypher.healthAssistant.dto.ResetPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.UserRegisterRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.dto.UserUpdateRequestDTO;
import bda.cypher.healthAssistant.service.AuthService;
import bda.cypher.healthAssistant.service.EmailOtpService;
import bda.cypher.healthAssistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final EmailOtpService emailOtpService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserRegisterRequestDTO request) {
        UserResponseDTO user = userService.registerUser(request);
        AuthResponseDTO response = new AuthResponseDTO(true, "Qeydiyyat uğurludur", null, null, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(Authentication authentication) {
        UserResponseDTO user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@Valid @RequestBody UserUpdateRequestDTO request,
                                                    Authentication authentication) {
        UserResponseDTO user = userService.updateUser(authentication.getName(), request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/email/otp/send")
    public ResponseEntity<EmailOtpSendResponseDTO> sendEmailOtp(@Valid @RequestBody EmailOtpSendRequestDTO request,
                                                                HttpServletRequest httpRequest) {
        EmailOtpSendResponseDTO resp = new EmailOtpSendResponseDTO(0L, java.time.Instant.now(), java.time.Instant.now(), 0);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/email/otp/verify")
    public ResponseEntity<EmailOtpVerifyResponseDTO> verifyEmailOtp(@Valid @RequestBody EmailOtpVerifyRequestDTO request) {
        EmailOtpVerifyResponseDTO resp = new EmailOtpVerifyResponseDTO("", java.time.Instant.now());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request,
                                                             HttpServletRequest httpRequest) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.forgotPassword(request, ip, userAgent));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
