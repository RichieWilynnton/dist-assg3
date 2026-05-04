package dto;

public abstract class AuthRequest extends BaseRequest {
    protected AuthRequest(String username) {
        super(username);
    }
}
