package server;

public class UserInfoEntry {
    public String username;
    public String password;
    public int numGames;
    public int numWins;
    public float avgTimeToWin;
    public int rank;

    public UserInfoEntry(String username, String password, int numGames, int numWins, float avgTimeToWin, int rank) {
        this.username = username;
        this.password = password;
        this.numGames = numGames;
        this.numWins = numWins;
        this.avgTimeToWin = avgTimeToWin;
        this.rank = rank;
    }
}
