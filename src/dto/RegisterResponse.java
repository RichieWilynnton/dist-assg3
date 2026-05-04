package dto;

public class RegisterResponse extends AuthResponse {
    public RegisterResponse(boolean success, String message) {
        super(success, message);
    }
}
