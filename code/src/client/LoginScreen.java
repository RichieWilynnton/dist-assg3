package client;

import dto.LoginRequest;
import dto.LoginResponse;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import server.AuthService;

public class LoginScreen extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;

    public LoginScreen() {
        super("Authentication");

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        initUi();
        bindActions();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.LINE_END;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(380, 200));
        pack();
        setLocationRelativeTo(null);
    }

    private void bindActions() {
        Runnable onLogout = () -> {
            ClientSession.clear();
            passwordField.setText("");
            setVisible(true);
        };

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a username.",
                        "Login",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String passsword = new String(passwordField.getPassword());
            if (passsword.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a password.",
                        "Login",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            AuthService authService = ClientApp.authService;
            try {
                LoginResponse response = authService.login(new LoginRequest(username, passsword));
                if (!response.success) {
                    JOptionPane.showMessageDialog(this,
                            "Login failed: " + response.message,
                            "Login",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Login failed. Failed to connect to server.",
                        "Login",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ClientSession.username = username;

            DashboardFrame dashboardFrame = new DashboardFrame(onLogout);
            dashboardFrame.setVisible(true);
            setVisible(false);
        });

        registerButton.addActionListener(e -> {
            Runnable onRegisterCancel = () -> {
                setVisible(true);
            };
            RegisterScreen registerScreen = new RegisterScreen(onRegisterCancel, onLogout);
            registerScreen.setVisible(true);
            setVisible(false);
        });
    }

}
