package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.AuthResponseDTO;
import bda.cypher.healthAssistant.dto.ForgotPasswordRequestDTO;
import bda.cypher.healthAssistant.dto.LoginRequestDTO;
import bda.cypher.healthAssistant.dto.MessageResponseDTO;
import bda.cypher.healthAssistant.dto.RefreshTokenRequestDTO;
import bda.cypher.healthAssistant.dto.ResetPasswordRequestDTO;

public interface AuthService {
    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);
    MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request, String ip, String userAgent);
    MessageResponseDTO resetPassword(ResetPasswordRequestDTO request);
}
