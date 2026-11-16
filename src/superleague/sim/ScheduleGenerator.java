package superleague.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import superleague.model.Team;

public final class ScheduleGenerator {
    private ScheduleGenerator() {
    }

    public static List<List<Fixture>> doubleRoundRobin(List<Team> teams, Random random) {
        List<Team> order = new ArrayList<>(teams);
        Collections.shuffle(order, random);
        int n = order.size();
        int half = n - 1;
        List<List<Fixture>> rounds = new ArrayList<>();
        Map<Team, Integer> streak = new HashMap<>();
        Map<Team, Integer> homes = new HashMap<>();

        for (int r = 0; r < half; r++) {
            List<Fixture> round = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Team a = order.get(i);
                Team b = order.get(n - 1 - i);
                int la = streak.getOrDefault(a, 0);
                int lb = streak.getOrDefault(b, 0);
                boolean bHome = la != lb ? la > lb
                        : !homes.getOrDefault(a, 0).equals(homes.getOrDefault(b, 0))
                        ? homes.getOrDefault(a, 0) > homes.getOrDefault(b, 0)
                        : random.nextBoolean();
                Team home = bHome ? b : a;
                Team away = bHome ? a : b;
                streak.put(home, Math.max(0, streak.getOrDefault(home, 0)) + 1);
                streak.put(away, Math.min(0, streak.getOrDefault(away, 0)) - 1);
                homes.merge(home, 1, Integer::sum);
                round.add(new Fixture(r + 1, home, away));
            }
            rounds.add(round);
            Team last = order.remove(n - 1);
            order.add(1, last);
        }

        for (int r = 0; r < half; r++) {
            List<Fixture> round = new ArrayList<>();
            for (Fixture f : rounds.get(r)) {
                round.add(new Fixture(half + r + 1, f.away(), f.home()));
            }
            rounds.add(round);
        }
        return rounds;
    }
}
