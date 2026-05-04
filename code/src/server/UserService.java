package server;

import dto.GetLeaderboardRequest;
import dto.GetLeaderboardResponse;
import dto.GetProfileRequest;
import dto.GetProfileResponse;
import dto.LogGameRequest;
import dto.LogGameResponse;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserService extends Remote {
    GetProfileResponse getProfile(GetProfileRequest request) throws RemoteException;
    GetLeaderboardResponse getLeaderboard(GetLeaderboardRequest request) throws RemoteException;
    LogGameResponse logGame(LogGameRequest request) throws RemoteException;
}
