package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.entity.RefreshToken;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.exception.UnauthorizedException;
import bda.cypher.healthAssistant.repository.RefreshTokenRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.security.JwtService;
import bda.cypher.healthAssistant.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshExpirationMs = refreshExpirationMs;
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

        storedToken.setRevokedAt(now);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);
        UserResponseDTO userDto = mapToDTO(user);
        return new AuthResponseDTO(true, "Token yeniləndi", newAccessToken, newRefreshToken, userDto);
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
