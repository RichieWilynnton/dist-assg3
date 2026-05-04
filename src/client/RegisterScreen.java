package client;

import dto.LoginRequest;
import dto.LoginResponse;
import dto.RegisterRequest;
import dto.RegisterResponse;
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

public class RegisterScreen extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JButton registerButton;
    private final JButton cancelButton;

    public RegisterScreen(Runnable onRegisterCancel, Runnable onLogout) {
        super("Register");

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        confirmPasswordField = new JPasswordField(18);
        registerButton = new JButton("Register");
        cancelButton = new JButton("Cancel");

        initUi();
        bindActions(onRegisterCancel, onLogout);
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
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

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(new JLabel("Confirm Password:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(confirmPasswordField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(380, 230));
        pack();
        setLocationRelativeTo(null);
    }

    private void bindActions(Runnable onRegisterCancel, Runnable onLogout) {
        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a username.",
                        "Register",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a password.",
                        "Register",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this,
                        "Passwords do not match.",
                        "Register",
                        JOptionPane.ERROR_MESSAGE);
                confirmPasswordField.setText("");
                return;
            }

            try {
                RegisterResponse response = ClientApp.authService.register(new RegisterRequest(username, password));
                if (!response.success) {
                    JOptionPane.showMessageDialog(this,
                            "Registration failed: " + response.message,
                            "Register",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Registration failed. Failed to connect to server.",
                        "Register",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            dispose();

            try {
                LoginResponse response = ClientApp.authService.login(new LoginRequest(username, password));
                if (!response.success) {
                    JOptionPane.showMessageDialog(this,
                            "Login failed after registration: " + response.message,
                            "Login",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Login failed after registration. Failed to connect to server.",
                        "Login",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ClientSession.username = username;

            DashboardFrame dashboardFrame = new DashboardFrame(onLogout);
            dashboardFrame.setVisible(true);
        });

        cancelButton.addActionListener(e -> {
            dispose();
            onRegisterCancel.run();
        });
    }
}
