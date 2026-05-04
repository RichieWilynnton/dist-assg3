package dto;

import java.io.Serializable;

public abstract class BaseRequest implements Serializable {
    public String username;

    protected BaseRequest(String username) {
        this.username = username;
    }
}
