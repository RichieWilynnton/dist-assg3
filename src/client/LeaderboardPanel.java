package client;

import dto.GetLeaderboardRequest;
import dto.GetLeaderboardResponse;
import dto.GetProfileResponse;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class LeaderboardPanel extends JPanel {
    public LeaderboardPanel() {
        super(new BorderLayout());
        buildUi();
    }

    private void buildUi() {
        GetLeaderboardResponse resp = null;
        try {
            resp = ClientApp.userService.getLeaderboard(new GetLeaderboardRequest(ClientSession.username));
        } catch (Exception e) {
            // fall through to error display
        }

        if (resp == null || !resp.success) {
            add(new JLabel("Failed to fetch leaderboard."));
            return;
        }

        add(buildLeaderboardTable(resp.entries), BorderLayout.CENTER);
    }

    private JScrollPane buildLeaderboardTable(List<GetProfileResponse> entries) {
        String[] columns = {"Rank", "Name", "Wins", "Games", "Avg Time to Win"};

        Object[][] data = new Object[entries.size()][5];
        for (int i = 0; i < entries.size(); i++) {
            GetProfileResponse e = entries.get(i);
            data[i][0] = e.rank;
            data[i][1] = e.username;
            data[i][2] = e.numWins;
            data[i][3] = e.numGames;
            data[i][4] = e.avgTimeToWin;
        }

        DefaultTableModel tableModel = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        return new JScrollPane(table);
    }
}