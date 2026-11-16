package superleague.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import superleague.data.SquadLoader;
import superleague.data.TeamData;
import superleague.game.Ending;
import superleague.game.Reward;
import superleague.game.RoundReport;
import superleague.game.Season;
import superleague.game.Verdict;
import superleague.model.Player;
import superleague.model.Tactic;
import superleague.model.Team;
import superleague.model.TrainingIntensity;
import superleague.sim.Fixture;
import superleague.sim.MatchResult;

public class ConsoleUI {
    private static final String LINE = "─".repeat(60);

    private final Scanner scanner;
    private final Random random;
    private final boolean auto;
    private final int autoTeamIndex;
    private boolean fastForward;

    public ConsoleUI(Scanner scanner, long seed, boolean auto, int autoTeamIndex) {
        this.scanner = scanner;
        this.random = new Random(seed);
        this.auto = auto;
        this.autoTeamIndex = autoTeamIndex;
        this.fastForward = auto;
    }

    public void run() {
        List<Team> teams = TeamData.createTeams();
        SquadLoader.Result data = SquadLoader.populate(teams, random);

        printTitle();
        System.out.println("[선수 데이터] " + data.summary());
        data.warnings().forEach(w -> System.out.println("  · " + w));
        Team userTeam = chooseTeam(teams);
        Season season = new Season(teams, userTeam, random);

        System.out.println();
        System.out.printf("%s 감독으로 부임했습니다.%n", userTeam.name());
        System.out.printf("운영진 목표: %d위 이내 (전력 등급 %s)%n", userTeam.tier().targetRank(), userTeam.tier());
        System.out.println("17라운드 후 중간 평가, 34라운드 후 시즌 종료 평가가 있습니다.");

        while (!season.isFinished()) {
            if (!fastForward) {
                roundMenu(season);
            }
            RoundReport report = season.playNextRound();
            printRoundReport(season, report);
            if (report.round() == Season.MID_SEASON_ROUND) {
                review(season, "중간 평가");
            }
        }

        printStandings(season);
        review(season, "시즌 종료 평가");
        printEnding(season);
    }

    private void printTitle() {
        System.out.println(LINE);
        System.out.println("  SUPER LEAGUE MANAGER  ·  슈퍼리그 감독 시뮬레이션");
        System.out.println(LINE);
    }

    private Team chooseTeam(List<Team> teams) {
        System.out.println("감독으로 부임할 구단을 고르세요.");
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            System.out.printf("%2d. %s %s  목표 %d위 이내%n", i + 1,
                    Text.padRight(t.name(), 20), t.tier(), t.tier().targetRank());
        }
        if (auto) {
            Team t = teams.get(autoTeamIndex);
            System.out.printf("> %d (자동 선택)%n", autoTeamIndex + 1);
            return t;
        }
        return teams.get(readInt("번호", 1, teams.size()) - 1);
    }

    private void roundMenu(Season season) {
        Team team = season.userTeam();
        while (true) {
            Fixture next = season.nextUserFixture();
            Team opponent = next.opponentOf(team);
            String venue = next.home() == team ? "홈" : "원정";
            System.out.println();
            System.out.println(LINE);
            String rank = season.roundsPlayed() == 0 ? "개막 전" : "현재 " + season.rankOf(team) + "위";
            System.out.printf("%d라운드 / %d  ·  %s  ·  다음 상대: %s (%s, %s등급)%n",
                    season.roundsPlayed() + 1, season.totalRounds(), rank,
                    opponent.name(), venue, opponent.tier());
            System.out.printf("스카우트 리포트: 상대는 주로 '%s' 전술을 씁니다. (유리한 대응: %s)%n",
                    opponent.preferredTactic().label(), opponent.preferredTactic().family().beatenBy().label());
            printTeamStatus(team);
            System.out.println("1. 경기 진행");
            System.out.println("2. 훈련 강도 변경");
            System.out.println("3. 전술 변경");
            System.out.println("4. 순위표");
            System.out.println("5. 선수단");
            System.out.println("6. 시즌 끝까지 자동 진행");
            int choice = readInt("선택", 1, 6);
            switch (choice) {
                case 1 -> {
                    return;
                }
                case 2 -> chooseIntensity(team);
                case 3 -> chooseTactic(team);
                case 4 -> printStandings(season);
                case 5 -> printSquad(team);
                default -> {
                    fastForward = true;
                    return;
                }
            }
        }
    }

    private void printTeamStatus(Team team) {
        System.out.printf("사기      %s %+4d  (경기력 %+.1f%%)%n",
                Text.bar(team.morale(), -100, 100, 20), team.morale(), team.moraleModifier() * 100);
        System.out.printf("훈련 수준 %s %4d  (경기력 %+.1f%%, 상한 %d)%n",
                Text.bar(team.training(), 0, 100, 20), team.training(), team.trainingModifier() * 100, team.trainingCap());
        System.out.printf("구단 시설 %s %4d  (경기력 %+.1f%%)%n",
                Text.bar(team.facility(), 0, Team.MAX_FACILITY, 20), team.facility(), team.facilityModifier() * 100);
        System.out.printf("평균 컨디션 %.0f · 부상자 %d명 · 훈련: %s · 전술: %s%n",
                team.averageCondition(), team.injuredCount(), team.intensity().label(), team.tactic().label());
        System.out.printf("예상 경기력 %.1f%n", team.performance());
    }

    private void chooseIntensity(Team team) {
        TrainingIntensity[] values = TrainingIntensity.values();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s%n", i + 1, values[i].summary());
        }
        team.setIntensity(values[readInt("훈련 강도", 1, values.length) - 1]);
        System.out.printf("이번 주부터 %s을 진행합니다.%n", team.intensity().label());
    }

    private void chooseTactic(Team team) {
        Tactic[] values = Tactic.values();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s (득점 x%.2f, 실점 x%.2f, '%s' 상대로 유리)%n", i + 1,
                    values[i].label(), values[i].attackFactor(), values[i].concedeFactor(),
                    values[i].beatsLabel());
        }
        team.setTactic(values[readInt("전술", 1, values.length) - 1]);
        System.out.printf("전술을 %s(으)로 바꿨습니다.%n", team.tactic().label());
    }

    private void printRoundReport(Season season, RoundReport report) {
        Team team = season.userTeam();
        MatchResult r = report.userResult();
        int gf = r.goalsFor(team);
        int ga = r.goalsAgainst(team);
        String outcome = gf > ga ? "승" : gf == ga ? "무" : "패";
        if (fastForward) {
            System.out.printf("R%-2d %s  %s  (%d위, 사기 %+d)%n", report.round(), outcome, r.scoreLine(),
                    season.rankOf(team), team.morale());
            report.news().forEach(n -> System.out.println("    · " + n));
            return;
        }
        System.out.println();
        System.out.printf("[%d라운드 결과] %s%n", report.round(), r.scoreLine());
        if (!r.homeScorers().isEmpty()) {
            System.out.printf("  %s 득점: %s%n", r.fixture().home().name(), String.join(", ", r.homeScorers()));
        }
        if (!r.awayScorers().isEmpty()) {
            System.out.printf("  %s 득점: %s%n", r.fixture().away().name(), String.join(", ", r.awayScorers()));
        }
        System.out.println("  다른 경기:");
        for (MatchResult other : report.results()) {
            if (other != r) {
                System.out.println("    " + other.scoreLine());
            }
        }
        for (String n : report.news()) {
            System.out.println("  [소식] " + n);
        }
        System.out.printf("  현재 %d위 · 사기 %+d%n", season.rankOf(team), team.morale());
    }

    private void review(Season season, String title) {
        Team team = season.userTeam();
        int rank = season.rankOf(team);
        Verdict verdict = Verdict.of(team.tier(), rank);
        System.out.println();
        System.out.println(LINE);
        System.out.printf("[%s] %d위 · 목표 %d위 이내 → %s%n", title, rank, team.tier().targetRank(), verdict.label());
        if (verdict == Verdict.MET) {
            System.out.println("운영진이 보상을 제안합니다. 하나를 고르세요.");
        } else if (verdict == Verdict.OUTSTANDING) {
            System.out.println("기대를 크게 넘어선 성적에 운영진이 시설 투자와 보너스를 모두 결정했습니다.");
        }
        List<String> messages = season.boardReview(t -> chooseReward());
        messages.forEach(m -> System.out.println("  · " + m));
        if (verdict == Verdict.NEAR) {
            System.out.println("  · 운영진은 기대 수준이라며 별도 조치를 하지 않았습니다.");
        }
    }

    private Reward chooseReward() {
        Reward[] values = Reward.values();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s — %s%n", i + 1, values[i].label(), values[i].effect());
        }
        if (auto) {
            System.out.println("> 1 (자동 선택)");
            return values[0];
        }
        return values[readInt("보상", 1, values.length) - 1];
    }

    private void printStandings(Season season) {
        Team user = season.userTeam();
        System.out.println();
        System.out.println(LINE);
        System.out.println("순위  " + Text.padRight("구단", 20) + "등급  경기  승  무  패  득실  승점");
        List<Team> table = season.standings();
        for (int i = 0; i < table.size(); i++) {
            Team t = table.get(i);
            String mark = t == user ? "▶" : " ";
            System.out.printf("%s%2d   %s %s   %3d %3d %3d %3d  %+4d  %3d%n", mark, i + 1,
                    Text.padRight(t.name(), 20), t.tier(), t.played(), t.won(), t.drawn(), t.lost(),
                    t.goalDifference(), t.points());
        }
    }

    private void printSquad(Team team) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("포지션  " + Text.padRight("이름", 22) + "나이  현재  최대  성장  컨디션  출전  득점  상태");
        List<Player> xi = team.startingEleven();
        team.squad().stream()
                .sorted(Comparator.comparing(Player::position).thenComparing(Comparator.comparingInt(Player::rating).reversed()))
                .forEach(p -> System.out.printf("%-6s  %s %3d  %4d  %4d  %4s  %6d  %4d  %4d  %s%n",
                        p.position().label(), Text.padRight(p.isGenerated() ? p.name() + " *" : p.name(), 22),
                        p.age(), p.rating(), p.potential(), p.growth() > 0 ? "+" + p.growth() : "",
                        p.condition(), p.appearances(), p.goals(),
                        !p.isAvailable() ? "부상(" + p.injuredRounds() + ")" : xi.contains(p) ? "선발" : ""));
    }

    private void printEnding(Season season) {
        Team team = season.userTeam();
        int rank = season.rankOf(team);
        Ending ending = Ending.decide(team, rank);
        long wins = team.won();
        System.out.println();
        System.out.println(LINE);
        System.out.printf("  ENDING · %s%n", ending.title());
        System.out.println(LINE);
        System.out.println(ending.description());
        System.out.printf("최종 %d위 · %d승 %d무 %d패 · 득실 %+d · 승점 %d%n",
                rank, wins, team.drawn(), team.lost(), team.goalDifference(), team.points());
        Player star = ending.star();
        if (ending.starStays()) {
            System.out.printf("에이스 %s(능력 %d)는 잔류를 선언했습니다.%n", star.name(), star.rating());
        } else {
            System.out.printf("선수단 분위기가 나빠 에이스 %s(능력 %d)가 이적을 요청했습니다.%n", star.name(), star.rating());
        }
        Player topScorer = team.squad().stream().max(Comparator.comparingInt(Player::goals)).orElseThrow();
        System.out.printf("팀 득점왕: %s (%d골)%n", topScorer.name(), topScorer.goals());
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.printf("%s (%d-%d) > ", prompt, min, max);
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.out.println("입력이 끝나 게임을 종료합니다.");
                System.exit(0);
            }
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("올바른 번호를 입력하세요.");
        }
    }
}
