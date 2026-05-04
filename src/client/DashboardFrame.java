package client;

import dto.LogGameRequest;
import dto.LogGameResponse;
import dto.LogoutRequest;
import dto.LogoutResponse;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import server.AuthService;

public class DashboardFrame extends JFrame {
    private static final String PROFILE_CARD = "profile";
    private static final String LEADERBOARD_CARD = "leaderboard";

    private final Runnable onLogout;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;

    public DashboardFrame(Runnable onLogout) {
        super("Game Dashboard");
        this.onLogout = onLogout;
        this.cardLayout = new CardLayout();
        this.contentPanel = new JPanel(cardLayout);

        initUi();
        bindMenuActions();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        contentPanel.add(new UserProfilePanel(), PROFILE_CARD);
        add(contentPanel, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
        cardLayout.show(contentPanel, PROFILE_CARD);

        setPreferredSize(new Dimension(700, 420));
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenuItem userProfileItem = new JMenuItem("User Profile");
        userProfileItem.setActionCommand("profile");

        JMenuItem playGameItem = new JMenuItem("Play Game");
        playGameItem.setActionCommand("play");

        JMenuItem leaderboardItem = new JMenuItem("Leaderboard");
        leaderboardItem.setActionCommand("leaderboard");

        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.setActionCommand("logout");

        menuBar.add(userProfileItem);
        menuBar.add(playGameItem);
        menuBar.add(leaderboardItem);
        menuBar.add(logoutItem);

        return menuBar;
    }

    private void bindMenuActions() {
        JMenuBar menuBar = getJMenuBar();
        for (int i = 0; i < menuBar.getComponentCount(); i++) {
            if (menuBar.getComponent(i) instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) menuBar.getComponent(i);
                item.addActionListener(e -> handleAction(e.getActionCommand()));
            }
        }
    }

    private void handleAction(String actionCommand) {
        if ("profile".equals(actionCommand)) {
            contentPanel.removeAll();
            contentPanel.add(new UserProfilePanel(), PROFILE_CARD);
            cardLayout.show(contentPanel, PROFILE_CARD);
            contentPanel.revalidate();
            contentPanel.repaint();
            return;
        }

        if ("leaderboard".equals(actionCommand)) {
            contentPanel.removeAll();
            contentPanel.add(new LeaderboardPanel(), LEADERBOARD_CARD);
            cardLayout.show(contentPanel, LEADERBOARD_CARD);
            contentPanel.revalidate();
            contentPanel.repaint();
            return;
        }

        if ("play".equals(actionCommand)) {
            try {
                LogGameResponse response = ClientApp.userService.logGame(new LogGameRequest(ClientSession.username));
                if (!response.success) {
                    JOptionPane.showMessageDialog(this, "Failed to record game: " + response.message, "Play Game", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to connect to server. " + e.getMessage(), "Play Game", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        if ("logout".equals(actionCommand)) {
            AuthService authService = ClientApp.authService;
            try {
                LogoutResponse response = authService.logout(new LogoutRequest(ClientSession.username));
                if (!response.success) {
                    // THIS SHOULD NOT HAPPEN
                }
                
            } catch (Exception e) {
                System.err.println("Logout failed: " + e.getMessage());
            }

            dispose();
            onLogout.run();
        }
    }
}
