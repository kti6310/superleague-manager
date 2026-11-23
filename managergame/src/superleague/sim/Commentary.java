package superleague.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Team;
import superleague.sim.MatchEvent.Side;
import superleague.sim.MatchEvent.Type;

public final class Commentary {

    private record Draft(int minute, int extra, Side side, Type type, Player actor, Player other, boolean own) {
        Draft(int minute, int extra, Side side, Type type, Player actor, Player other) {
            this(minute, extra, side, type, actor, other, false);
        }

        int order() {
            return MatchEvent.order(minute, extra, type);
        }
    }

    public record Result(List<MatchEvent> events, int possession, int homeCorners, int awayCorners, int stoppage) {
    }

    private static final double OWN_GOAL_CHANCE = 0.04;

    private final Random random;
    private final Team home;
    private final Team away;
    private final List<Player> homeXi;
    private final List<Player> awayXi;
    private int cut = -1;
    private int stoppage;

    private Commentary(Random random, Team home, Team away, List<Player> homeXi, List<Player> awayXi) {
        this.random = random;
        this.home = home;
        this.away = away;
        this.homeXi = homeXi;
        this.awayXi = awayXi;
    }

    public static Result build(Random random, Fixture fixture, List<Player> homeXi, List<Player> awayXi,
                               List<Player> homeScorers, List<Player> awayScorers,
                               double homeExpected, double awayExpected, double homePower, double awayPower,
                               int cut, int startHome, int startAway, int stoppage) {
        Commentary c = new Commentary(random, fixture.home(), fixture.away(), homeXi, awayXi);
        c.cut = cut;
        c.stoppage = stoppage > 0 ? stoppage : 2 + random.nextInt(4);
        return c.run(homeScorers, awayScorers, homeExpected, awayExpected, homePower, awayPower, startHome, startAway);
    }

    static double remainingFraction(int cut) {
        if (cut < 0) {
            return 1.0;
        }
        int minute = cut >= 450 && cut <= 459 ? 45 : cut / 10;
        return Math.max(0, 90 - minute) / 90.0;
    }

    private Result run(List<Player> homeScorers, List<Player> awayScorers, double homeExp, double awayExp,
                       double homePower, double awayPower, int startHome, int startAway) {
        double frac = remainingFraction(cut);
        List<Draft> drafts = new ArrayList<>();
        int[] corners = new int[2];

        for (int s = 0; s < 2; s++) {
            Side side = s == 0 ? Side.HOME : Side.AWAY;
            List<Player> xi = s == 0 ? homeXi : awayXi;
            List<Player> opp = s == 0 ? awayXi : homeXi;
            List<Player> scorers = s == 0 ? homeScorers : awayScorers;
            double exp = s == 0 ? homeExp : awayExp;

            for (Player scorer : scorers) {
                int[] t = goalTime();
                if (random.nextDouble() < OWN_GOAL_CHANCE) {
                    Player defender = pick(opp, this::foulWeight, keeper(opp));
                    Player crosser = random.nextDouble() < 0.6 ? pick(xi, this::assistWeight, null) : null;
                    drafts.add(new Draft(t[0], t[1], side, Type.GOAL, defender, crosser, true));
                    continue;
                }
                Player assist = random.nextDouble() < 0.75 ? pick(xi, this::assistWeight, scorer) : null;
                drafts.add(new Draft(t[0], t[1], side, Type.GOAL, scorer, assist));
            }
            int extraShots = poisson(frac * (3 + 4.5 * exp));
            int saves = Math.min(extraShots, poisson(frac * (1.0 + 1.5 * exp)));
            Player keeper = keeper(opp);
            for (int i = 0; i < extraShots; i++) {
                int[] t = at(1, 89);
                if (t == null) {
                    continue;
                }
                Player shooter = pick(xi, this::scoringWeight, null);
                Type type;
                if (i < saves) {
                    type = random.nextDouble() < 0.3 ? Type.BIG_SAVE : Type.SAVE;
                } else {
                    double r = random.nextDouble();
                    type = r < 0.09 ? Type.WOODWORK : r < 0.40 ? Type.BLOCK : Type.MISS;
                }
                drafts.add(new Draft(t[0], t[1], side, type, shooter, keeper));
            }
            int y = poisson(1.5 * frac);
            for (int i = 0; i < y; i++) {
                int[] t = at(10, 89);
                if (t != null) {
                    drafts.add(new Draft(t[0], 0, side, Type.YELLOW, pick(xi, this::foulWeight, null), null));
                }
            }
            int[] redAt = random.nextDouble() < 0.02 * frac ? at(56, 89) : null;
            if (redAt != null) {
                drafts.add(new Draft(redAt[0], 0, side, Type.RED, pick(xi, this::foulWeight, null), null));
            }
            corners[s] = poisson(frac * (3 + 2 * exp));
        }

        int possession = (int) Math.round(50 + (homePower - awayPower) * 1.2
                + (home.tactic().attackFactor() - away.tactic().attackFactor()) * 18);
        possession = Math.max(30, Math.min(70, possession));

        int infos = (int) Math.round(frac * (5 + random.nextInt(3)));
        for (int i = 0; i < infos; i++) {
            boolean homeFlow = random.nextInt(100) < possession;
            int[] t = at(4, 87);
            if (t != null) {
                drafts.add(new Draft(t[0], 0, homeFlow ? Side.HOME : Side.AWAY, Type.INFO, null, null));
            }
        }
        if (cut < 0) {
            drafts.add(new Draft(0, 0, Side.NONE, Type.KICKOFF, null, null));
        }
        if (cut < 459) {
            drafts.add(new Draft(45, 9, Side.NONE, Type.HALFTIME, null, null));
        }
        drafts.add(new Draft(90, stoppage, Side.NONE, Type.FULLTIME, null, null));
        drafts.sort(Comparator.comparingInt(Draft::order).thenComparing(d -> d.type() == Type.HALFTIME || d.type() == Type.FULLTIME));

        List<MatchEvent> events = new ArrayList<>();
        int h = startHome;
        int a = startAway;
        for (Draft d : drafts) {
            if (d.type() == Type.GOAL) {
                if (d.side() == Side.HOME) {
                    h++;
                } else {
                    a++;
                }
            }
            int extra = d.type() == Type.HALFTIME ? 0 : d.extra();
            events.add(new MatchEvent(d.minute(), extra, d.side(), d.type(), text(d, h, a, possession), h, a,
                    d.actor(), d.own()));
        }
        return new Result(events, possession, corners[0], corners[1], stoppage);
    }

    private String text(Draft d, int h, int a, int possession) {
        Team team = d.side() == Side.AWAY ? away : home;
        Team other = d.side() == Side.AWAY ? home : away;
        String score = String.format("(%s %d-%d %s)", home.name(), h, a, away.name());
        Player p = d.actor();
        Player o = d.other();
        return switch (d.type()) {
            case KICKOFF -> String.format("킥오프! %s 대 %s, 휘슬이 울렸습니다.", home.name(), away.name());
            case HALFTIME -> String.format("전반 종료. %s", score);
            case FULLTIME -> String.format("경기 종료! 최종 스코어 %s", score);
            case GOAL -> (d.own() ? ownGoalText(p, o, other) : goalText(p, o, team)) + " " + score;
            case SAVE -> one(
                    String.format("%s의 슈팅, %s 정면으로 향합니다.", p.name(), keeperName(o)),
                    String.format("%s 중거리 슛! %s 안정적으로 잡아냅니다.", subj(p.name()), subj(keeperName(o))),
                    String.format("%s 낮게 깔아 찬 슈팅을 %s 막아냅니다.", josa(p.name(), "의"), subj(keeperName(o))));
            case BIG_SAVE -> one(
                    String.format("결정적인 기회! %s 일대일 슈팅을 %s 몸을 날려 막아냅니다!", josa(p.name(), "의"), subj(keeperName(o))),
                    String.format("%s 노마크 헤더! 그러나 %s 손끝으로 쳐냅니다. 엄청난 선방!", subj(p.name()), subj(keeperName(o))),
                    String.format("%s 골문 구석을 노렸지만 %s 슈퍼 세이브!", subj(p.name()), subj(keeperName(o))));
            case MISS -> one(
                    String.format("%s 슈팅은 골대를 크게 벗어납니다.", josa(p.name(), "의")),
                    String.format("%s 감아 찬 슛이 골문 옆으로 살짝 빗나갑니다.", subj(p.name())),
                    String.format("%s 발리 슈팅은 크로스바 위로 넘어갑니다.", josa(p.name(), "의")));
            case BLOCK -> one(
                    String.format("%s 슈팅이 수비 몸에 맞고 굴절됩니다.", josa(p.name(), "의")),
                    String.format("%s 박스 안 슈팅, %s 수비가 몸을 던져 막습니다.", josa(p.name(), "의"), other.name()));
            case WOODWORK -> one(
                    String.format("%s 슈팅이 골대를 강타합니다! %s 가슴을 쓸어내립니다.", josa(p.name(), "의"), subj(other.name())),
                    String.format("크로스바! %s 회심의 슈팅이 골대에 맞고 나옵니다.", josa(p.name(), "의")));
            case YELLOW -> one(
                    String.format("%s 거친 태클로 옐로카드를 받습니다.", subj(p.name())),
                    String.format("%s 경고. 역습을 끊는 반칙이었습니다.", josa(p.name(), "에게")));
            case RED -> String.format("레드카드! %s 퇴장당합니다. %s 수적 열세에 놓입니다.", subj(p.name()), subj(team.name()));
            case INFO -> flow(team, other, possession);
            default -> "";
        };
    }

    private String goalText(Player scorer, Player assist, Team team) {
        String finish = scorer.height() >= 186 && random.nextDouble() < 0.5
                ? "헤더로 골망을 흔듭니다"
                : one("왼쪽 구석으로 정확히 찔러 넣습니다", "침착하게 골키퍼를 제치고 밀어 넣습니다",
                "강력한 슈팅으로 골망을 흔듭니다", "감아 차기로 골문 상단을 꿰뚫습니다",
                "골키퍼 다리 사이로 차 넣습니다");
        if (assist == null) {
            return String.format("골! %s 개인 돌파에 이어 %s! %s 환호합니다.", josa(scorer.name(), "의"), finish,
                    subj(team.name()));
        }
        return String.format("골! %s 패스를 받은 %s %s!", josa(assist.name(), "의"), subj(scorer.name()), finish);
    }

    private String ownGoalText(Player defender, Player crosser, Team defenderTeam) {
        if (crosser == null) {
            return String.format("자책골! 문전 혼전 속에 %s 걷어낸 공이 자기 골문으로 들어갑니다. %s 망연자실합니다.",
                    subj(defender.name()), subj(defenderTeam.name()));
        }
        return String.format("자책골! %s 크로스가 %s 발에 맞고 그대로 골문 안으로 들어갑니다.",
                josa(crosser.name(), "의"), josa(defender.name(), "의"));
    }

    private String flow(Team team, Team other, int possession) {
        return one(
                String.format("%s 점유율을 높이며 상대 진영에서 공을 돌립니다.", subj(team.name())),
                String.format("%s 측면을 집요하게 공략합니다.", subj(team.name())),
                String.format("%s 높은 압박에 %s 빌드업이 흔들립니다.", josa(team.name(), "의"), josa(other.name(), "의")),
                String.format("%s 빠른 역습으로 기회를 엿봅니다.", subj(team.name())),
                String.format("중원 싸움이 치열합니다. %s 조금씩 주도권을 잡아갑니다.", subj(team.name())));
    }

    private String one(String... options) {
        return options[random.nextInt(options.length)];
    }

    private int[] goalTime() {
        for (int i = 0; i < 40; i++) {
            double r = random.nextDouble();
            int[] t = r < 0.05 ? new int[]{45, 1 + random.nextInt(3)}
                    : r < 0.12 ? new int[]{90, 1 + random.nextInt(stoppage)}
                    : new int[]{1 + random.nextInt(89), 0};
            if (MatchEvent.order(t[0], t[1], Type.GOAL) > cut) {
                return t;
            }
        }
        // shortcut: a goal left after a very late substitution lands in the last stoppage minute
        return new int[]{90, stoppage};
    }

    private int[] at(int lo, int hi) {
        int from = Math.max(lo, cut < 0 ? lo : (cut >= 450 && cut <= 459 ? 46 : cut / 10 + 1));
        if (from > hi) {
            return null;
        }
        return new int[]{from + random.nextInt(hi - from + 1), 0};
    }

    private Player pick(List<Player> xi, ToDoubleFunction<Player> weight, Player exclude) {
        double total = 0;
        for (Player p : xi) {
            if (p != exclude) {
                total += weight.applyAsDouble(p);
            }
        }
        double roll = random.nextDouble() * total;
        for (Player p : xi) {
            if (p == exclude) {
                continue;
            }
            roll -= weight.applyAsDouble(p);
            if (roll <= 0) {
                return p;
            }
        }
        return xi.get(0);
    }

    private static Player keeper(List<Player> xi) {
        return xi.stream().filter(p -> p.position() == Position.GK).findFirst().orElse(xi.isEmpty() ? null : xi.get(0));
    }

    private static String keeperName(Player keeper) {
        return keeper == null ? "골키퍼" : keeper.name();
    }

    private double scoringWeight(Player p) {
        return switch (p.position()) {
            case FW -> 6.0;
            case MF -> 2.5;
            case DF -> 0.8;
            case GK -> 0.01;
        };
    }

    private double assistWeight(Player p) {
        double base = switch (p.position()) {
            case FW -> 3.0;
            case MF -> 4.0;
            case DF -> 1.2;
            case GK -> 0.05;
        };
        superleague.model.TacticalRole duty = home.dutyOf(p);
        if (duty == null) {
            duty = away.dutyOf(p);
        }
        return base * (duty == null ? 1 : duty.assist) * p.attribute(superleague.model.Attributes.PASSING) / 150.0;
    }

    private double foulWeight(Player p) {
        return switch (p.position()) {
            case FW -> 1.0;
            case MF -> 2.5;
            case DF -> 3.0;
            case GK -> 0.1;
        };
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

    static boolean hasBatchim(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char c = word.charAt(word.length() - 1);
        if (c >= 0xAC00 && c <= 0xD7A3) {
            return (c - 0xAC00) % 28 != 0;
        }
        if (Character.isDigit(c)) {
            return "0136789".indexOf(c) >= 0;
        }
        return false;
    }

    static String subj(String word) {
        return word + (hasBatchim(word) ? "이" : "가");
    }

    static String josa(String word, String kind) {
        return switch (kind) {
            case "을" -> word + (hasBatchim(word) ? "을" : "를");
            case "은" -> word + (hasBatchim(word) ? "은" : "는");
            case "와" -> word + (hasBatchim(word) ? "과" : "와");
            default -> word + kind;
        };
    }
}
