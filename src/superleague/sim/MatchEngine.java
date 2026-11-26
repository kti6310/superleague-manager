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
    private static final double EDGE_NORM = 0.98;
    private static final double EDGE_EXPONENT = 1.5;
    private static final double ENDURANCE_MID = 145;

    private final Random random;

    public MatchEngine(Random random) {
        this.random = random;
    }

    public MatchResult play(Fixture fixture) {
        return start(fixture).finish();
    }

    long nextSeed() {
        return random.nextLong();
    }

    public LiveMatch start(Fixture fixture) {
        return new LiveMatch(this, fixture);
    }

    Commentary.Result simulate(Fixture fixture, int cut, int startHome, int startAway, int stoppage,
                               double homeFitness, double awayFitness) {
        Team home = fixture.home();
        Team away = fixture.away();
        List<Player> homeXi = home.startingEleven();
        List<Player> awayXi = away.startingEleven();

        double homePower = home.performance() * (fixture.neutral() ? 1 : HOME_ADVANTAGE) * homeFitness;
        double awayPower = away.performance() * awayFitness;

        double homeExpected = BASE_GOALS * Math.pow(homePower / awayPower, STRENGTH_EXPONENT)
                * home.tactic().attackFactor() * away.tactic().concedeFactor()
                * home.instructions().attackFactor() * away.instructions().concedeFactor()
                * aerialFactor(home, away)
                * home.dutyAttackFactor() * away.dutyConcedeFactor()
                * (1 + home.teamTraining().attack / 100) * (1 - away.teamTraining().defense / 100)
                * home.managerAttackFactor(away) * away.managerConcedeFactor(home);
        double awayExpected = BASE_GOALS * Math.pow(awayPower / homePower, STRENGTH_EXPONENT)
                * away.tactic().attackFactor() * home.tactic().concedeFactor()
                * away.instructions().attackFactor() * home.instructions().concedeFactor()
                * aerialFactor(away, home)
                * away.dutyAttackFactor() * home.dutyConcedeFactor()
                * (1 + away.teamTraining().attack / 100) * (1 - home.teamTraining().defense / 100)
                * away.managerAttackFactor(home) * home.managerConcedeFactor(away);

        homeExpected *= attributeEdge(home, away);
        awayExpected *= attributeEdge(away, home);

        if (home.tactic().counters(away.tactic())) {
            homeExpected *= COUNTER_BONUS + home.counterBonusExtra();
        } else if (away.tactic().counters(home.tactic())) {
            awayExpected *= COUNTER_BONUS + away.counterBonusExtra();
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
        double diff = aerialSkill(attacker) - aerialSkill(defender);
        double effect = 0.006 * diff * attacker.instructions().aerialWeight();
        return 1 + Math.max(-0.06, Math.min(0.06, effect));
    }

    // Front-line finishing/dribbling/pace against back-line defending/physique/pace, so attributes matter beyond overall rating.
    static double attributeEdge(Team attack, Team defense) {
        double ratio = attackQuality(attack) / defenseQuality(defense) / EDGE_NORM;
        return Math.max(0.7, Math.min(1.4, Math.pow(ratio, EDGE_EXPONENT)));
    }

    // Weighted mix of detailed attributes, e.g. skill(p, "패스", 2, "시야", 1).
    public static double skill(Player p, Object... nameWeight) {
        double sum = 0;
        double weight = 0;
        for (int i = 0; i < nameWeight.length; i += 2) {
            double w = ((Number) nameWeight[i + 1]).doubleValue();
            sum += w * p.detail((String) nameWeight[i]);
            weight += w;
        }
        return sum / weight;
    }

    static double attackValue(Player p) {
        return skill(p, "골 결정력", 25, "침착성", 10, "오프 더 볼", 15, "드리블", 10, "퍼스트 터치", 10,
                "패스", 10, "시야", 10, "가속도", 5, "판단력", 5);
    }

    static double defenseValue(Player p) {
        if (p.position() == superleague.model.Position.GK) {
            return skill(p, "반사 신경", 30, "핸들링", 20, "일대일", 20, "공중볼 장악", 15, "집중력", 15);
        }
        return skill(p, "태클", 20, "일대일 마크", 20, "수비 위치 선정", 20, "집중력", 10, "예측력", 10, "몸싸움", 10,
                "주력", 5, "팀워크", 5);
    }

    static double attackQuality(Team t) {
        double sum = 0;
        double weight = 0;
        for (Player p : t.startingEleven()) {
            double w = switch (p.position()) {
                case FW -> 1.0;
                case MF -> 0.6;
                case DF -> 0.2;
                case GK -> 0;
            };
            sum += w * attackValue(p);
            weight += w;
        }
        return weight == 0 ? 1 : sum / weight;
    }

    static double defenseQuality(Team t) {
        double sum = 0;
        double weight = 0;
        for (Player p : t.startingEleven()) {
            double w = switch (p.position()) {
                case DF -> 1.0;
                case MF -> 0.6;
                case FW -> 0.1;
                case GK -> 1.5;
            };
            sum += w * defenseValue(p);
            weight += w;
        }
        return weight == 0 ? 1 : sum / weight;
    }

    // Average heading/leap of the outfield XI, on roughly a centimetre scale like the old height-only measure.
    static double aerialSkill(Team t) {
        return t.startingEleven().stream().filter(p -> p.position() != superleague.model.Position.GK)
                .mapToDouble(p -> skill(p, "헤더", 1, "점프 거리", 1) / 2).average().orElse(70);
    }

    // Stamina, natural fitness and work rate set how fast a player tires; veterans tire a bit faster.
    public static double endurance(Player p) {
        double stamina = skill(p, "지구력", 2, "타고난 체력", 2, "활동량", 1);
        double m = 1 + (ENDURANCE_MID - stamina) / 150.0 + Math.max(0, p.age() - 30) * 0.02;
        return Math.max(0.8, Math.min(1.3, m));
    }

    public static int fatigueFor(Team team, Player p) {
        int base = 15 + team.instructions().extraFatigue();
        int pressExtra = team.instructions().pressing() - 3;
        if (pressExtra > 0 && p.bmi() > 24) {
            base += pressExtra;
        }
        TacticalRole duty = team.dutyOf(p);
        double tactic = switch (team.tactic()) {
            case VERY_ATTACKING -> 1.15;
            case ATTACKING -> 1.08;
            case BALANCED -> 1.0;
            case DEFENSIVE -> 0.95;
            case VERY_DEFENSIVE -> 0.9;
        };
        int raw = base + (duty == null ? 0 : duty.fatigue) + team.teamTraining().fatigue;
        return Math.max(6, (int) Math.round(raw * tactic * endurance(p)));
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
        return positionWeight * skill(p, "골 결정력", 6, "침착성", 2, "오프 더 볼", 2) / 150.0;
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
