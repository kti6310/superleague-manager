package superleague.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import superleague.model.Player;
import superleague.model.Team;

public record GroupDraw(List<Team[]> pots, List<Team> groupA, List<Team> groupB) {

    public static double strength(Team team) {
        return team.squad().stream().mapToInt(Player::rating).boxed()
                .sorted(Comparator.reverseOrder()).limit(11).mapToInt(Integer::intValue).average().orElse(0);
    }

    static GroupDraw of(List<Team> teams, Random random) {
        List<Team> seeded = new ArrayList<>(teams);
        seeded.sort(Comparator.comparingDouble(GroupDraw::strength).reversed());
        List<Team[]> pots = new ArrayList<>();
        List<Team> a = new ArrayList<>();
        List<Team> b = new ArrayList<>();
        for (int i = 0; i + 1 < seeded.size(); i += 2) {
            Team x = seeded.get(i);
            Team y = seeded.get(i + 1);
            if (random.nextBoolean()) {
                Team t = x;
                x = y;
                y = t;
            }
            pots.add(new Team[]{x, y});
            a.add(x);
            b.add(y);
        }
        return new GroupDraw(pots, a, b);
    }

    public double averageA() {
        return groupA.stream().mapToDouble(GroupDraw::strength).average().orElse(0);
    }

    public double averageB() {
        return groupB.stream().mapToDouble(GroupDraw::strength).average().orElse(0);
    }
}
