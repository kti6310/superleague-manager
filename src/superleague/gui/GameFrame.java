package superleague.gui;

import java.awt.Dimension;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import superleague.data.SquadLoader;
import superleague.data.TeamData;
import superleague.game.Season;
import superleague.model.ManagerStyle;
import superleague.model.Team;

public class GameFrame extends JFrame {
    private String managerName;
    private ManagerStyle managerStyle;

    public GameFrame() {
        super("Super League Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setIconImage(Logo.icon(64));
        showStart();
        setLocationRelativeTo(null);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new GameFrame().setVisible(true);
        });
    }

    private void showStart() {
        show(new TitlePanel(this::showProfile));
    }

    private void showProfile() {
        show(new ManagerPanel((name, style) -> {
            managerName = name;
            managerStyle = style;
            show(new StartPanel(this::startGame));
        }, this::showStart));
    }

    private void show(java.awt.Container panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    private void startGame(int teamIndex, Long seed) {
        Random random = new Random(seed != null ? seed : System.currentTimeMillis());
        List<Team> teams = TeamData.createTeams();
        SquadLoader.Result data = SquadLoader.populate(teams, random);
        if (!data.warnings().isEmpty()) {
            showWarnings(data);
        }
        teams.get(teamIndex).setManager(managerName, managerStyle);
        Season season = new Season(teams, teams.get(teamIndex), random);
        setContentPane(new GamePanel(season, data, this::showStart));
        revalidate();
        repaint();
    }

    private void showWarnings(SquadLoader.Result data) {
        StringBuilder sb = new StringBuilder(data.summary()).append("\n\n");
        List<String> w = data.warnings();
        int shown = Math.min(15, w.size());
        for (int i = 0; i < shown; i++) {
            sb.append("· ").append(w.get(i)).append('\n');
        }
        if (w.size() > shown) {
            sb.append(String.format("· 외 %d건 (소식 탭에서 전체 확인)%n", w.size() - shown));
        }
        Dialogs.showScroll(this, "선수 데이터", "확인이 필요한 항목이 있습니다", sb.toString().trim());
    }
}
