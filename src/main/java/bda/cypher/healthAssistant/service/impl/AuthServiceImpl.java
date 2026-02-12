package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.ForgotPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.MessageResponseDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;
import bda.cypher.healthAssistant.dto.ResetPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.entity.PasswordResetToken;
import bda.cypher.healthAssistant.entity.RefreshToken;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.exception.UnauthorizedException;
import bda.cypher.healthAssistant.repository.PasswordResetTokenRepository;
import bda.cypher.healthAssistant.repository.RefreshTokenRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.security.JwtService;
import bda.cypher.healthAssistant.service.AuthService;
import bda.cypher.healthAssistant.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final long refreshExpirationMs;
    private final long resetTokenExpirationMinutes;
    private final String appFrontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           MailService mailService,
                           @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs,
                           @Value("${app.reset-token-expiration-minutes}") long resetTokenExpirationMinutes,
                           @Value("${app.frontend-url}") String appFrontendUrl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.refreshExpirationMs = refreshExpirationMs;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
        this.appFrontendUrl = appFrontendUrl;
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email və ya şifrə yanlışdır"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Email və ya şifrə yanlışdır");
        }

        String token = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);
        UserResponseDTO userDto = mapToDTO(user);
        return new AuthResponseDTO(true, "Giriş uğurludur", token, refreshToken, userDto);
    }

    @Override
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String rawToken = request.getRefreshToken();
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token etibarsızdır"));

        Instant now = Instant.now();
        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token etibarsızdır");
        }

        storedToken.setLastUsedAt(now);
        storedToken.setRevokedAt(now);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);
        UserResponseDTO userDto = mapToDTO(user);
        return new AuthResponseDTO(true, "Token yeniləndi", newAccessToken, newRefreshToken, userDto);
    }

    @Override
    @Transactional
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request, String ip, String userAgent) {
        String message = "Əgər email doğrudursa, link göndərildi";
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return new MessageResponseDTO(true, message);
        }

        Instant now = Instant.now();
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findAllByUserIdAndUsedAtIsNull(user.getId());
        for (PasswordResetToken token : activeTokens) {
            token.setUsedAt(now);
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        String rawToken = generateResetTokenValue();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plusSeconds(resetTokenExpirationMinutes * 60L));
        resetToken.setIp(ip);
        resetToken.setUserAgent(userAgent);
        passwordResetTokenRepository.save(resetToken);

        String baseUrl = appFrontendUrl.endsWith("/") ? appFrontendUrl.substring(0, appFrontendUrl.length() - 1) : appFrontendUrl;
        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        try {
            mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (Exception ignored) {
        }

        return new MessageResponseDTO(true, message);
    }

    @Override
    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new RuntimeException("Reset token etibarsızdır"));

        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("Reset token etibarsızdır");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);

        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        for (RefreshToken refreshToken : refreshTokens) {
            refreshToken.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(refreshTokens);

        return new MessageResponseDTO(true, "Şifrə yeniləndi");
    }

    private UserResponseDTO mapToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setHeight(user.getHeight());
        dto.setWeight(user.getWeight());
        dto.setSeverity(user.getSeverity());

        if (user.getHealthCondition() != null) {
            dto.setHealthCondition(user.getHealthCondition().getName());
            if (user.getHealthCondition().getCategory() != null) {
                dto.setConditionCategory(user.getHealthCondition().getCategory().getName());
            }
        }

        return dto;
    }

    private String createRefreshToken(User user) {
        String rawToken = generateRefreshTokenValue();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        Instant now = Instant.now();
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(token);
        return rawToken;
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateResetTokenValue() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Token hash xətası");
        }
    }
}
