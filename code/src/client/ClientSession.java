package client;

public final class ClientSession {
    public static String username;

    private ClientSession() {}

    public static void clear() {
        username = null;
    }
}
