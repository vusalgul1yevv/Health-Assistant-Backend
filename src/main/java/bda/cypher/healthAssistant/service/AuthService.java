package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;

public interface AuthService {
    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);
}
