package dto;

public abstract class AuthResponse extends BaseResponse {
    protected AuthResponse(boolean success, String message) {
        super(success, message);
    }
}
