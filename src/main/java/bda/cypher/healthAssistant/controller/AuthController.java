package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.ForgotPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.MessageResponseDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;
import bda.cypher.healthAssistant.dto.ResetPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.UserRegisterRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.service.AuthService;
import bda.cypher.healthAssistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

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
