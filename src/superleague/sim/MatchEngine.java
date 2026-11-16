package superleague.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import superleague.model.Player;
import superleague.model.Attributes;
import superleague.model.TacticalRole;
import superleague.model.Team;

public class MatchEngine {
    private static final double BASE_GOALS = 1.35;
    private static final double STRENGTH_EXPONENT = 2.5;
    private static final double HOME_ADVANTAGE = 1.04;
    private static final double COUNTER_BONUS = 1.25;

    private final Random random;

    public MatchEngine(Random random) {
        this.random = random;
    }

    public MatchResult play(Fixture fixture) {
        return start(fixture).finish();
    }

    public LiveMatch start(Fixture fixture) {
        return new LiveMatch(this, fixture);
    }

    Commentary.Result simulate(Fixture fixture, int cut, int startHome, int startAway, int stoppage) {
        Team home = fixture.home();
        Team away = fixture.away();
        List<Player> homeXi = home.startingEleven();
        List<Player> awayXi = away.startingEleven();

        double homePower = home.performance() * HOME_ADVANTAGE;
        double awayPower = away.performance();

        double homeExpected = BASE_GOALS * Math.pow(homePower / awayPower, STRENGTH_EXPONENT)
                * home.tactic().attackFactor() * away.tactic().concedeFactor()
                * home.instructions().attackFactor() * away.instructions().concedeFactor()
                * aerialFactor(home, away)
                * home.dutyAttackFactor() * away.dutyConcedeFactor()
                * (1 + home.teamTraining().attack / 100) * (1 - away.teamTraining().defense / 100);
        double awayExpected = BASE_GOALS * Math.pow(awayPower / homePower, STRENGTH_EXPONENT)
                * away.tactic().attackFactor() * home.tactic().concedeFactor()
                * away.instructions().attackFactor() * home.instructions().concedeFactor()
                * aerialFactor(away, home)
                * away.dutyAttackFactor() * home.dutyConcedeFactor()
                * (1 + away.teamTraining().attack / 100) * (1 - home.teamTraining().defense / 100);

        if (home.tactic().counters(away.tactic())) {
            homeExpected *= COUNTER_BONUS;
        } else if (away.tactic().counters(home.tactic())) {
            awayExpected *= COUNTER_BONUS;
        }

        double frac = Commentary.remainingFraction(cut);
        int homeGoals = poisson(homeExpected * frac);
        int awayGoals = poisson(awayExpected * frac);

        List<Player> homeScorers = pickScorers(home, homeXi, homeGoals);
        List<Player> awayScorers = pickScorers(away, awayXi, awayGoals);
        return Commentary.build(new Random(random.nextLong()), fixture, homeXi, awayXi,
                homeScorers, awayScorers, homeExpected, awayExpected, homePower, awayPower,
                cut, startHome, startAway, stoppage);
    }

    public static double aerialFactor(Team attacker, Team defender) {
        double diff = attacker.aerialHeight() - defender.aerialHeight();
        double effect = 0.006 * diff * attacker.instructions().aerialWeight();
        return 1 + Math.max(-0.06, Math.min(0.06, effect));
    }

    public static int fatigueFor(Team team, Player p) {
        int base = 15 + team.instructions().extraFatigue();
        int pressExtra = team.instructions().pressing() - 3;
        if (pressExtra > 0 && p.bmi() > 24) {
            base += pressExtra;
        }
        TacticalRole duty = team.dutyOf(p);
        return Math.max(8, base + (duty == null ? 0 : duty.fatigue) + team.teamTraining().fatigue);
    }

    private List<Player> pickScorers(Team team, List<Player> xi, int goals) {
        List<Player> scorers = new ArrayList<>();
        double total = xi.stream().mapToDouble(p -> scoringWeight(team, p)).sum();
        for (int g = 0; g < goals; g++) {
            double roll = random.nextDouble() * total;
            for (Player p : xi) {
                roll -= scoringWeight(team, p);
                if (roll <= 0) {
                    scorers.add(p);
                    break;
                }
            }
        }
        return scorers;
    }

    private double scoringWeight(Team team, Player p) {
        TacticalRole duty = team.dutyOf(p);
        return scoringWeight(p) * (duty == null ? 1 : duty.scorer);
    }

    private double scoringWeight(Player p) {
        double positionWeight = switch (p.position()) {
            case FW -> 6.0;
            case MF -> 2.5;
            case DF -> 0.8;
            case GK -> 0.01;
        };
        return positionWeight * p.attribute(Attributes.SHOOTING) / 150.0;
    }

    private int poisson(double lambda) {
        double limit = Math.exp(-lambda);
        double product = 1.0;
        int k = 0;
        do {
            k++;
            product *= random.nextDouble();
        } while (product > limit);
        return k - 1;
    }
}
