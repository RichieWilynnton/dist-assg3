package server;

public class ServerApp {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        try {
            DatabaseService db = new DatabaseService();
            AuthService authService = new AuthServiceImpl(db);
            UserService userService = new UserServiceImpl(db);

            // Initialize the JMS game server before installing the security manager
            // so GlassFish's HK2 service loader is not blocked by the restricted policy.
            JPokerGameServer gameServer = new JPokerGameServer(host);

            System.setSecurityManager(new SecurityManager());
            java.rmi.Naming.rebind("AuthService", authService);
            java.rmi.Naming.rebind("UserService", userService);
            System.out.println("RMI services bound.");
            Thread gameThread = new Thread(() -> {
                try {
                    System.out.println("JPoker game server listening for lobby joins...");
                    gameServer.run();
                } catch (Exception e) {
                    System.err.println("JPoker game server error: " + e.getMessage());
                }
            }, "jpoker-game-server");
            gameThread.setDaemon(true);
            gameThread.start();

            System.out.println("Server is running...");
        } catch (Exception e) {
            System.err.println("Exception thrown: " + e);
        }
    }
}
