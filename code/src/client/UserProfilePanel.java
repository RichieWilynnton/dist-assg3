package client;

import dto.GetProfileRequest;
import dto.GetProfileResponse;
import dto.UpdatePasswordRequest;
import dto.UpdatePasswordResponse;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class UserProfilePanel extends JPanel {
    public UserProfilePanel() {
        super(new GridBagLayout());
        buildUi();
    }

    private void buildUi() {
        String username = ClientSession.username;
        int numWins = 0, numGames = 0, rank = 0;
        float avgTimeToWin = 0.0f;

        boolean fetchFailed = false;

        try {
            GetProfileResponse resp = ClientApp.userService.getProfile(new GetProfileRequest(username));
            if (resp.success) {
                numWins = resp.numWins;
                numGames = resp.numGames;
                avgTimeToWin = resp.avgTimeToWin;
                rank = resp.rank;
            } else {
                fetchFailed = true;
            }
        } catch (Exception e) {
            fetchFailed = true;
        }

        if (fetchFailed) {
            add(new JLabel("Failed to fetch profile."));
            return;
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        add(new JLabel(username), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Number of Wins:"), gbc);

        gbc.gridx = 1;
        add(new JLabel(String.valueOf(numWins)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Number of Games:"), gbc);

        gbc.gridx = 1;
        add(new JLabel(String.valueOf(numGames)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Average Time to Win:"), gbc);

        gbc.gridx = 1;
        add(new JLabel(String.valueOf(avgTimeToWin)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("Rank:"), gbc);

        gbc.gridx = 1;
        add(new JLabel(String.valueOf(rank)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton updatePasswordBtn = new JButton("Update Password");
        updatePasswordBtn.addActionListener(e -> showUpdatePasswordDialog());
        add(updatePasswordBtn, gbc);
    }

    private void showUpdatePasswordDialog() {
        JPasswordField currentPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Current password:"), gbc);
        gbc.gridx = 1;
        panel.add(currentPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("New password:"), gbc);
        gbc.gridx = 1;
        panel.add(newPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Confirm new password:"), gbc);
        gbc.gridx = 1;
        panel.add(confirmPassField, gbc);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Update Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String currentPassword = new String(currentPassField.getPassword());
        String newPassword = new String(newPassField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());

        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "New password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            UpdatePasswordResponse resp = ClientApp.authService.updatePassword(
                    new UpdatePasswordRequest(ClientSession.username, currentPassword, newPassword));
            if (resp.success) {
                JOptionPane.showMessageDialog(this, "Password updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, resp.message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}