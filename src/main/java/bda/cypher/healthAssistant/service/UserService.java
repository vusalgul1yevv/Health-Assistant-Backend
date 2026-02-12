package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.UserRegisterRequestDTO;
import bda.cypher.healthAssistant.dto.UserResponseDTO;
import bda.cypher.healthAssistant.dto.UserUpdateRequestDTO;


public interface UserService {
    UserResponseDTO registerUser(UserRegisterRequestDTO request);
    UserResponseDTO getUserByEmail(String email);
    UserResponseDTO updateUser(String email, UserUpdateRequestDTO request);
}
