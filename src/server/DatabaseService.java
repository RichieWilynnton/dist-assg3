package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseService {
    private static final String DB_HOST = "localhost";
    private static final String DB_USER = "c3358";
    private static final String DB_PASS = "c3358PASS";
    private static final String DB_NAME = "c3358_assg";

    private final Connection conn;

    public DatabaseService() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(
                "jdbc:mysql://" + DB_HOST + "/" + DB_NAME
                        + "?user=" + DB_USER
                        + "&password=" + DB_PASS);
    }

    public void insertNewUser(String username, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.execute();
    }

    public UserInfoEntry readUser(String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT username, password, num_games, num_wins, avg_time_to_win, leaderboard_rank" +
                " FROM users WHERE username = ?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return new UserInfoEntry(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getInt("num_games"),
                    rs.getInt("num_wins"),
                    rs.getFloat("avg_time_to_win"),
                    rs.getInt("leaderboard_rank"));
        }
        return null;
    }

    public void insertOnlineUser(String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO online_users (username) VALUES (?)");
        stmt.setString(1, username);
        stmt.execute();
    }

    public boolean isOnlineUser(String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT username FROM online_users WHERE username = ?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    public void deleteOnlineUser(String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM online_users WHERE username = ?");
        stmt.setString(1, username);
        stmt.executeUpdate();
    }

    public void updatePassword(String username, String newPassword) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET password = ? WHERE username = ?");
        stmt.setString(1, newPassword);
        stmt.setString(2, username);
        stmt.executeUpdate();
    }

    public void incrementNumGames(String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET num_games = num_games + 1 WHERE username = ?");
        stmt.setString(1, username);
        stmt.executeUpdate();
    }

    public java.util.List<UserInfoEntry> readLeaderboard() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT username, password, num_games, num_wins, avg_time_to_win, leaderboard_rank" +
                " FROM users ORDER BY leaderboard_rank ASC");
        ResultSet rs = stmt.executeQuery();
        java.util.List<UserInfoEntry> entries = new java.util.ArrayList<>();
        while (rs.next()) {
            entries.add(new UserInfoEntry(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getInt("num_games"),
                    rs.getInt("num_wins"),
                    rs.getFloat("avg_time_to_win"),
                    rs.getInt("leaderboard_rank")));
        }
        return entries;
    }
}
