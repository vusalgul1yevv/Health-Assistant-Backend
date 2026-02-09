package bda.cypher.healthAssistant.dto;

public class AuthResponseDTO {
    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private UserResponseDTO user;

    public AuthResponseDTO() {}

    public AuthResponseDTO(boolean success, String message, String token, String refreshToken, UserResponseDTO user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserResponseDTO getUser() { return user; }
    public void setUser(UserResponseDTO user) { this.user = user; }
}
