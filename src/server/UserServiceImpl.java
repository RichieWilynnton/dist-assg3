package server;

import dto.GetLeaderboardRequest;
import dto.GetLeaderboardResponse;
import dto.GetProfileRequest;
import dto.GetProfileResponse;
import dto.LogGameRequest;
import dto.LogGameResponse;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl extends UnicastRemoteObject implements UserService {
    private final DatabaseService db;

    public UserServiceImpl(DatabaseService db) throws RemoteException {
        this.db = db;
    }

    private boolean isAuthenticated(String username) throws SQLException {
        return db.isOnlineUser(username);
    }

    @Override
    public GetProfileResponse getProfile(GetProfileRequest request) throws RemoteException {
        try {
            if (!isAuthenticated(request.username)) {
                return new GetProfileResponse(false, "Not authenticated");
            }
            UserInfoEntry entry = db.readUser(request.username);
            if (entry == null) {
                return new GetProfileResponse(false, "User not found");
            }
            return new GetProfileResponse(true, "OK",
                    entry.username, entry.numGames, entry.numWins,
                    entry.avgTimeToWin, entry.rank);
        } catch (SQLException e) {
            System.err.println("Database error in getProfile: " + e.getMessage());
            return new GetProfileResponse(false, "Internal server error : " + e.getMessage());
        }
    }

    @Override
    public GetLeaderboardResponse getLeaderboard(GetLeaderboardRequest request) throws RemoteException {
        try {
            if (!isAuthenticated(request.username)) {
                return new GetLeaderboardResponse(false, "Not authenticated");
            }
            List<UserInfoEntry> entries = db.readLeaderboard();
            List<GetProfileResponse> result = new ArrayList<>();
            for (UserInfoEntry entry : entries) {
                result.add(new GetProfileResponse(true, "OK",
                        entry.username, entry.numGames, entry.numWins,
                        entry.avgTimeToWin, entry.rank));
            }
            return new GetLeaderboardResponse(true, "OK", result);
        } catch (SQLException e) {
            System.err.println("Database error in getLeaderboard: " + e.getMessage());
            return new GetLeaderboardResponse(false, "Internal server error : " + e.getMessage());
        }
    }

    @Override
    public LogGameResponse logGame(LogGameRequest request) throws RemoteException {
        try {
            if (!isAuthenticated(request.username)) {
                return new LogGameResponse(false, "Not authenticated");
            }
            db.incrementNumGames(request.username);
            return new LogGameResponse(true, "OK");
        } catch (SQLException e) {
            System.err.println("Database error in logGame: " + e.getMessage());
            return new LogGameResponse(false, "Internal server error : " + e.getMessage());
        }
    }
}
