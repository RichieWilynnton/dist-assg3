package dto;

public class LogoutResponse extends AuthResponse {
    public LogoutResponse(boolean success, String message) {
        super(success, message);
    }
}
