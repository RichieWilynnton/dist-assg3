package dto;

import java.io.Serializable;

public abstract class BaseResponse implements Serializable {
    public final boolean success;
    public final String message;

    protected BaseResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
