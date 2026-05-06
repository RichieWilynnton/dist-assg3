package client;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.util.Random;

public class PlayGamePanel extends JPanel {

    private static final String[] SUITS = { "clubs", "diamonds", "hearts", "spades" };
    private static final Random RAND = new Random();

    private JPokerGame gameClient;

    private JButton newGameButton;
    private JLabel  statusLabel;

    private JButton submitButton;
    private JLabel  gameStatusLabel;

    public PlayGamePanel() {
        super(new GridBagLayout());
        showLobbyView();
    }

    private void showLobbyView() {
        removeAll();
        submitButton    = null;
        gameStatusLabel = null;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(8, 8, 8, 8);
        gbc.gridx   = 0;
        gbc.anchor  = GridBagConstraints.CENTER;

        gbc.gridy = 0;
        add(new JLabel("Join the lobby and wait for players to start."), gbc);

        gbc.gridy = 1;
        statusLabel = new JLabel(" ");
        add(statusLabel, gbc);

        gbc.gridy = 2;
        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> sendNewGameRequest());
        add(newGameButton, gbc);

        revalidate();
        repaint();
    }

    private void showGameView(String[] players, int[] cards) {
        removeAll();
        newGameButton = null;
        statusLabel   = null;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 8, 6, 8);
        gbc.gridx   = 0;
        gbc.anchor  = GridBagConstraints.CENTER;

        gbc.gridy = 0;
        add(new JLabel("Players: " + String.join(", ", players)), gbc);

        gbc.gridy = 1;
        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        for (int card : cards) {
            String suit = SUITS[RAND.nextInt(SUITS.length)];
            String alias = cardAlias(card);
            JPanel cardBox = new JPanel(new java.awt.BorderLayout(0, 2));
            java.io.InputStream is = getClass().getResourceAsStream("/cards/" + cardFileName(card, suit));
            if (is != null) {
                try {
                    Image scaled = new ImageIcon(javax.imageio.ImageIO.read(is))
                            .getImage().getScaledInstance(80, 120, Image.SCALE_SMOOTH);
                    cardBox.add(new JLabel(new ImageIcon(scaled)), java.awt.BorderLayout.CENTER);
                    is.close();
                } catch (java.io.IOException ex) {
                    cardBox.add(new JLabel(String.valueOf(card)), java.awt.BorderLayout.CENTER);
                }
            } else {
                cardBox.add(new JLabel(String.valueOf(card)), java.awt.BorderLayout.CENTER);
            }
            if (alias != null) {
                JLabel aliasLabel = new JLabel("[" + alias + "]", JLabel.CENTER);
                cardBox.add(aliasLabel, java.awt.BorderLayout.SOUTH);
            }
            cardsPanel.add(cardBox);
        }
        add(cardsPanel, gbc);

        gbc.gridy = 2;
        add(new JLabel("Enter an expression using all 4 cards that equals 24:"), gbc);

        gbc.gridy = 3;
        add(new JLabel("(Face cards: J=11, Q=12, K=13 — either format accepted)"), gbc);

        gbc.gridy = 4;
        JTextField expressionField = new JTextField(22);
        add(expressionField, gbc);

        gbc.gridy = 5;
        gameStatusLabel = new JLabel(" ");
        add(gameStatusLabel, gbc);

        gbc.gridy = 6;
        submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            String expr = expressionField.getText().trim();
            if (expr.isEmpty()) return;
            submitButton.setEnabled(false);
            expressionField.setEnabled(false);
            gameStatusLabel.setText("Waiting for everyone to submit...");
            submitAnswer(expr);
        });
        add(submitButton, gbc);

        revalidate();
        repaint();
    }

    private void sendNewGameRequest() {
        String username = ClientSession.username;
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You must be logged in to start a game.",
                    "Play Game", JOptionPane.ERROR_MESSAGE);
            return;
        }

        newGameButton.setEnabled(false);
        statusLabel.setText("Joining lobby...");

        try {
            gameClient = new JPokerGame(ClientApp.serverHost);
            gameClient.requestNewGame(username);
            statusLabel.setText("Waiting for players...");

            gameClient.subscribeToGameEvents(username, new GameListener() {
                @Override
                public void onGameStart(String[] players, int[] cards) {
                    showGameView(players, cards);
                }

                @Override
                public void onGameWinner(String winner, String expression) {
                    String msg = username.equals(winner)
                            ? "You won!\nExpression: " + expression
                            : winner + " won!\nExpression: " + expression;
                    JOptionPane.showMessageDialog(PlayGamePanel.this, msg,
                            "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    if (gameClient != null) {
                        gameClient.close();
                        gameClient = null;
                    }
                    showLobbyView();
                }

                @Override
                public void onNoWinner() {
                    JOptionPane.showMessageDialog(PlayGamePanel.this,
                            "Nobody got it this round!",
                            "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    if (gameClient != null) {
                        gameClient.close();
                        gameClient = null;
                    }
                    showLobbyView();
                }
            });

        } catch (Exception ex) {
            if (statusLabel   != null) statusLabel.setText(" ");
            if (newGameButton != null) newGameButton.setEnabled(true);
            if (gameClient    != null) { gameClient.close(); gameClient = null; }
            JOptionPane.showMessageDialog(this, "Failed to send game request: " + ex.getMessage(),
                    "Play Game", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitAnswer(String expression) {
        try {
            gameClient.submitAnswer(ClientSession.username, expression);
        } catch (Exception ex) {
            if (submitButton    != null) submitButton.setEnabled(true);
            if (gameStatusLabel != null) gameStatusLabel.setText("Failed to submit.");
            JOptionPane.showMessageDialog(this, "Failed to submit answer: " + ex.getMessage(),
                    "Play Game", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String cardAlias(int value) {
        if (value == 11) return "J";
        if (value == 12) return "Q";
        if (value == 13) return "K";
        return null;
    }

    private String cardFileName(int value, String suit) {
        if (value == 11) return "jack_of_" + suit + ".png";
        if (value == 12) return "queen_of_" + suit + ".png";
        if (value == 13) return "king_of_" + suit + ".png";
        return value + "_of_" + suit + ".png";
    }
}

