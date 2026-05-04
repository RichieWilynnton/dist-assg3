package server;

import dto.LoginRequest;
import dto.LoginResponse;
import dto.LogoutRequest;
import dto.LogoutResponse;
import dto.RegisterRequest;
import dto.RegisterResponse;
import dto.UpdatePasswordRequest;
import dto.UpdatePasswordResponse;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private final DatabaseService db;

    public AuthServiceImpl(DatabaseService db) throws RemoteException {
        this.db = db;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            UserInfoEntry userInfo = db.readUser(loginRequest.username);
            if (userInfo == null) {
                return new LoginResponse(false, "User not found");
            }

            if (!userInfo.password.equals(loginRequest.password)) {
                return new LoginResponse(false, "Incorrect password");
            }

            if (db.isOnlineUser(loginRequest.username)) {
                return new LoginResponse(false, "User already logged in");
            }

            db.insertOnlineUser(loginRequest.username);
            System.out.println("User logged in: " + loginRequest.username);
            return new LoginResponse(true, "Login successful");
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            return new LoginResponse(false, "Internal server error " + e.getMessage());
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        try {
            if (db.readUser(registerRequest.username) != null) {
                return new RegisterResponse(false, "Username already exists");
            }

            db.insertNewUser(registerRequest.username, registerRequest.password);
            System.out.println("New user registered: " + registerRequest.username);
            return new RegisterResponse(true, "Registration successful");
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            return new RegisterResponse(false, "Internal server error : " + e.getMessage());
        }
    }

    @Override
    public LogoutResponse logout(LogoutRequest logoutRequest) {
        try {
            db.deleteOnlineUser(logoutRequest.username);
            System.out.println("User logged out: " + logoutRequest.username);
            return new LogoutResponse(true, "Logout successful");
        } catch (SQLException e) {
            System.err.println("Database error during logout: " + e.getMessage());
            return new LogoutResponse(false, "Internal server error : " + e.getMessage());
        }
    }

    @Override
    public UpdatePasswordResponse updatePassword(UpdatePasswordRequest request) {
        try {
            UserInfoEntry userInfo = db.readUser(request.username);
            if (userInfo == null) {
                return new UpdatePasswordResponse(false, "User not found");
            }
            if (!userInfo.password.equals(request.currentPassword)) {
                return new UpdatePasswordResponse(false, "Current password is incorrect");
            }
            if (request.newPassword == null || request.newPassword.isEmpty()) {
                return new UpdatePasswordResponse(false, "New password cannot be empty");
            }
            db.updatePassword(request.username, request.newPassword);
            System.out.println("Password updated for user: " + request.username);
            return new UpdatePasswordResponse(true, "Password updated successfully");
        } catch (SQLException e) {
            System.err.println("Database error during password update: " + e.getMessage());
            return new UpdatePasswordResponse(false, "Internal server error: " + e.getMessage());
        }
    }
}
