package superleague.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import superleague.model.Attributes;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Team;

public final class Shootout {
    public record Kick(Team team, Player taker, boolean scored, String text, int firstScore, int secondScore) {
    }

    public final Team first;
    public final Team second;
    private final List<Kick> kicks = new ArrayList<>();
    private Team winner;

    Shootout(Team first, Team second, Random random) {
        this.first = first;
        this.second = second;
        List<Player> takersA = takers(first);
        List<Player> takersB = takers(second);
        Player keeperA = keeper(first);
        Player keeperB = keeper(second);
        int a = 0;
        int b = 0;
        for (int i = 0; winner == null; i++) {
            boolean suddenDeath = i >= 5;
            boolean scoredA = shoot(random, takersA.get(i % takersA.size()), keeperB);
            a += scoredA ? 1 : 0;
            kicks.add(new Kick(first, takersA.get(i % takersA.size()), scoredA, text(random, takersA.get(i % takersA.size()), keeperB, scoredA), a, b));
            if (!suddenDeath && decided(a, b, i + 1, i)) {
                break;
            }
            boolean scoredB = shoot(random, takersB.get(i % takersB.size()), keeperA);
            b += scoredB ? 1 : 0;
            kicks.add(new Kick(second, takersB.get(i % takersB.size()), scoredB, text(random, takersB.get(i % takersB.size()), keeperA, scoredB), a, b));
            if (suddenDeath ? a != b : decided(a, b, i + 1, i + 1)) {
                break;
            }
        }
        winner = kicks.get(kicks.size() - 1).firstScore() > kicks.get(kicks.size() - 1).secondScore() ? first : second;
    }

    private static boolean decided(int a, int b, int takenA, int takenB) {
        return a + (5 - takenA) < b || b + (5 - takenB) < a;
    }

    public List<Kick> kicks() {
        return kicks;
    }

    public Team winner() {
        return winner;
    }

    public String score() {
        Kick last = kicks.get(kicks.size() - 1);
        return last.firstScore() + "-" + last.secondScore();
    }

    private static List<Player> takers(Team team) {
        List<Player> outfield = new ArrayList<>(team.startingEleven().stream().filter(p -> p.position() != Position.GK).toList());
        if (outfield.isEmpty()) {
            outfield.addAll(team.startingEleven());
        }
        outfield.sort(Comparator.comparingInt((Player p) -> p.attribute(Attributes.SHOOTING)).reversed());
        return outfield;
    }

    private static Player keeper(Team team) {
        return team.startingEleven().stream().filter(p -> p.position() == Position.GK).findFirst()
                .orElse(team.startingEleven().get(0));
    }

    private static boolean shoot(Random random, Player taker, Player keeper) {
        double p = 0.75 + (taker.attribute(Attributes.SHOOTING) - 150) / 600.0 - (keeper.rating() - 160) / 800.0;
        return random.nextDouble() < Math.max(0.55, Math.min(0.92, p));
    }

    private static String text(Random random, Player taker, Player keeper, boolean scored) {
        String[] goal = {"골! %s, 침착하게 골망을 흔듭니다!", "골! %s의 슛이 구석으로 정확히 꽂힙니다!",
                "골! %s, 골키퍼를 속이고 반대쪽으로 성공!", "골! %s, 강하게 차서 그대로 성공!"};
        String[] miss = {"실축! %s의 슛이 크로스바를 넘겼어요!", "실축! %s의 슛이 골포스트를 맞고 나옵니다!",
                "실축! %s의 슛이 골대 옆으로 벗어났어요!", "막혔습니다! " + keeper.name() + "이(가) %s의 슛을 선방!"};
        String[] pool = scored ? goal : miss;
        return String.format(pool[random.nextInt(pool.length)], taker.name());
    }
}
