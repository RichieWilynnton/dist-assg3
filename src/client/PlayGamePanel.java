package client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class PlayGamePanel extends JPanel {
    public PlayGamePanel() {
        super(new GridBagLayout());
        buildUi();
    }

    private void buildUi() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(new JLabel("Join the lobby and wait for 4 players to start."), gbc);

        gbc.gridy = 1;
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> sendNewGameRequest());
        add(newGameButton, gbc);
    }

    private void sendNewGameRequest() {
        String username = ClientSession.username;
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You must be logged in to start a game.", "Play Game", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (JPokerGame gameClient = new JPokerGame(ClientApp.serverHost)) {
            gameClient.requestNewGame(username);
            JOptionPane.showMessageDialog(
                    this,
                    "Lobby join request sent. The game will start when 4 players have joined.",
                    "Play Game",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to send game request: " + ex.getMessage(), "Play Game", JOptionPane.ERROR_MESSAGE);
        }
    }
}
