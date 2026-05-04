package dto;

public class GetProfileResponse extends BaseResponse {
    public final String username;
    public final int numGames;
    public final int numWins;
    public final float avgTimeToWin;
    public final int rank;

    public GetProfileResponse(boolean success, String message,
                              String username, int numGames, int numWins,
                              float avgTimeToWin, int rank) {
        super(success, message);
        this.username = username;
        this.numGames = numGames;
        this.numWins = numWins;
        this.avgTimeToWin = avgTimeToWin;
        this.rank = rank;
    }

    // Failure constructor
    public GetProfileResponse(boolean success, String message) {
        this(success, message, null, 0, 0, 0.0f, 0);
    }
}
