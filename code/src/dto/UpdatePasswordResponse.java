package dto;

public class UpdatePasswordResponse extends AuthResponse {
    public UpdatePasswordResponse(boolean success, String message) {
        super(success, message);
    }
}
