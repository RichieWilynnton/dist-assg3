package dto;

public class RegisterRequest extends AuthRequest {
    public String password;

    public RegisterRequest(String username, String password) {
        super(username);
        this.password = password;
    }
}
