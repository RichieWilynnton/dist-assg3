package dto;

import java.util.List;

public class GetLeaderboardResponse extends BaseResponse {
    public final List<GetProfileResponse> entries;

    public GetLeaderboardResponse(boolean success, String message, List<GetProfileResponse> entries) {
        super(success, message);
        this.entries = entries;
    }

    public GetLeaderboardResponse(boolean success, String message) {
        this(success, message, null);
    }
}
