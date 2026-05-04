package server;

import dto.LoginRequest;
import dto.LoginResponse;
import dto.LogoutRequest;
import dto.LogoutResponse;
import dto.RegisterRequest;
import dto.RegisterResponse;
import dto.UpdatePasswordRequest;
import dto.UpdatePasswordResponse;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    LoginResponse login(LoginRequest loginCredentials) throws RemoteException;
    RegisterResponse register(RegisterRequest RegistrationRequest) throws RemoteException;
    LogoutResponse logout(LogoutRequest loginCredentials) throws RemoteException;
    UpdatePasswordResponse updatePassword(UpdatePasswordRequest request) throws RemoteException;
}