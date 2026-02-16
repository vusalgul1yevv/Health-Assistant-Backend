package bda.cypher.healthAssistant.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import bda.cypher.healthAssistant.dto.UserRegisterRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.dto.UserUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.HealthCondition;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.HealthConditionRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.UserService;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final HealthConditionRepository healthConditionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO registerUser(UserRegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        user.setSeverity(request.getSeverity());
        user.setRole("USER");

        if (request.getConditionId() != null) {
            HealthCondition condition = healthConditionRepository.findById(request.getConditionId())
                    .orElseThrow(() -> new RuntimeException("Condition not found"));
            user.setHealthCondition(condition);
        }

        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
    }

    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
        return mapToDTO(user);
    }

    public UserResponseDTO updateUser(String email, UserUpdateRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null && !request.getGender().isBlank()) {
            user.setGender(request.getGender());
        }
        if (request.getHeight() != null) {
            user.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            user.setWeight(request.getWeight());
        }
        if (request.getSeverity() != null && !request.getSeverity().isBlank()) {
            user.setSeverity(request.getSeverity());
        }
        if (request.getConditionId() != null) {
            HealthCondition condition = healthConditionRepository.findById(request.getConditionId())
                    .orElseThrow(() -> new RuntimeException("Condition not found"));
            user.setHealthCondition(condition);
        }

        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
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
            dto.setConditionId(user.getHealthCondition().getId());
            dto.setHealthCondition(user.getHealthCondition().getName());
            if (user.getHealthCondition().getCategory() != null) {
                dto.setConditionCategoryId(user.getHealthCondition().getCategory().getId());
                dto.setConditionCategory(user.getHealthCondition().getCategory().getName());
            }
        }
        
        return dto;
    }
   
}
