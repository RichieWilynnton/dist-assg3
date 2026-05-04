package dto;

public class LoginRequest extends AuthRequest {
    public String password;

    public LoginRequest(String username, String password) {
        super(username);
        this.password = password;
    }
}
