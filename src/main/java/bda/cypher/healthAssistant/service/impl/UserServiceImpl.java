package bda.cypher.healthAssistant.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import bda.cypher.healthAssistant.dto.UserRegisterRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
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
