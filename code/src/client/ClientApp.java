package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.SwingUtilities;
import server.AuthService;
import server.UserService;

public final class ClientApp {
    public static AuthService authService;
    public static UserService userService;

    private ClientApp(String host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            authService = (AuthService) registry.lookup("AuthService");
            userService = (UserService) registry.lookup("UserService");
        } catch (Exception e) {
            System.err.println("Failed accessing RMI: " + e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ClientApp <server-host>");
            return;
        }

        new ClientApp(args[0]);

        SwingUtilities.invokeLater(() -> {
            LoginScreen screen = new LoginScreen();
            screen.setVisible(true);
        });
    }
}