package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.exception.UnauthorizedException;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.security.JwtService;
import bda.cypher.healthAssistant.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email və ya şifrə yanlışdır"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Email və ya şifrə yanlışdır");
        }

        String token = jwtService.generateToken(user);
        UserResponseDTO userDto = mapToDTO(user);
        return new AuthResponseDTO(true, "Giriş uğurludur", token, userDto);
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
}
