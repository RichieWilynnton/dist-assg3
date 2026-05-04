package dto;

public class UpdatePasswordRequest extends AuthRequest {
    public String currentPassword;
    public String newPassword;

    public UpdatePasswordRequest(String username, String currentPassword, String newPassword) {
        super(username);
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
