package dto;

public class LoginResponse extends AuthResponse {
    public LoginResponse(boolean success, String message) {
        super(success, message);
    }
}
