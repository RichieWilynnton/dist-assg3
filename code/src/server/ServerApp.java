package server;

public class ServerApp {

    public static void main(String[] args) {
        try {
            DatabaseService db = new DatabaseService();
            AuthService authService = new AuthServiceImpl(db);
            UserService userService = new UserServiceImpl(db);
            System.setSecurityManager(new SecurityManager());
            java.rmi.Naming.rebind("AuthService", authService);
            java.rmi.Naming.rebind("UserService", userService);
            System.out.println("Server is running...");
        } catch (Exception e) {
            System.err.println("Exception thrown: " + e);
        }
    }
}
