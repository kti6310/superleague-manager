package superleague.game;

import java.time.LocalDate;
import superleague.model.DayActivity;
import superleague.model.TeamTraining;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import superleague.model.Formation;
import superleague.model.Player;
import superleague.model.Tactic;
import superleague.model.Team;
import superleague.model.TrainingIntensity;
import superleague.sim.Fixture;
import superleague.sim.MatchEngine;
import java.util.function.Consumer;
import superleague.sim.LiveMatch;
import superleague.sim.MatchResult;
import superleague.sim.ScheduleGenerator;

public class Season {
    public static final int MID_SEASON_ROUND = 17;
    public static final int MAX_SEASONS = 5;
    public static final int OWNER_INTERVAL = 3;

    private final List<Team> teams;
    private final Team userTeam;
    private final Random random;
    private List<List<Fixture>> schedule;
    private int year = 0;
    private final List<String> career = new ArrayList<>();
    private int titles = 0;
    private int targetsMet = 0;
    private final MatchEngine engine;
    private final RandomEvents events;
    private final List<MatchResult> userResults = new ArrayList<>();
    private int roundsPlayed = 0;

    private static final LocalDate[] MATCH_DATES = java.util.stream.Stream.of(
            "2026-08-29", "2026-09-05", "2026-09-12", "2026-09-19", "2026-10-10", "2026-10-17", "2026-10-24",
            "2026-10-31", "2026-11-07", "2026-11-21", "2026-11-28", "2026-12-05", "2026-12-12", "2026-12-19",
            "2027-01-09", "2027-01-13", "2027-01-16", "2027-01-23", "2027-01-30", "2027-02-06", "2027-02-13",
            "2027-02-20", "2027-02-27", "2027-03-03", "2027-03-06", "2027-03-13", "2027-03-20", "2027-04-03",
            "2027-04-10", "2027-04-17", "2027-04-24", "2027-05-08", "2027-05-15", "2027-05-22")
            .map(LocalDate::parse).toArray(LocalDate[]::new);

    private LocalDate today = MATCH_DATES[0].minusDays(7);
    private OwnerReview pendingReview;
    private final java.util.Map<LocalDate, DayActivity> plan = new java.util.HashMap<>();

    public Season(List<Team> teams, Team userTeam, Random random) {
        this.teams = teams;
        this.userTeam = userTeam;
        this.random = random;
        this.schedule = ScheduleGenerator.doubleRoundRobin(teams, random);
        this.engine = new MatchEngine(random);
        this.events = new RandomEvents(random);
        motivate();
        Tactic[] styles = Tactic.values();
        for (Team team : teams) {
            team.setPreferredTactic(styles[random.nextInt(styles.length)]);
            chooseBestFormation(team);
            if (team != userTeam) {
                applyStyleInstructions(team);
            }
        }
    }

    public Team userTeam() {
        return userTeam;
    }

    public int roundsPlayed() {
        return roundsPlayed;
    }

    public LocalDate today() {
        return today;
    }

    public LocalDate dateOfRound(int index) {
        LocalDate d = index < MATCH_DATES.length ? MATCH_DATES[index]
                : MATCH_DATES[MATCH_DATES.length - 1].plusWeeks(index - MATCH_DATES.length + 1L);
        return d.plusDays(364L * year);
    }

    public int seasonNumber() {
        return year + 1;
    }

    public boolean isLastSeason() {
        return year + 1 >= MAX_SEASONS;
    }

    public String seasonLabel() {
        int start = MATCH_DATES[0].getYear() + year;
        return String.format("%d/%02d", start, (start + 1) % 100);
    }

    public List<String> career() {
        return career;
    }

    public int titles() {
        return titles;
    }

    public int targetsMet() {
        return targetsMet;
    }

    public void closeSeason(Player topScorer) {
        int rank = rankOf(userTeam);
        if (rank == 1) {
            titles++;
        }
        if (rank <= userTeam.tier().targetRank()) {
            targetsMet++;
        }
        career.add(String.format("%s · %d위%s · 승점 %d · %d승 %d무 %d패 · 득점왕 %s(%d골)", seasonLabel(), rank,
                rank == 1 ? " 우승" : "", userTeam.points(), userTeam.won(), userTeam.drawn(), userTeam.lost(),
                topScorer.name(), topScorer.goals()));
    }

    public List<String> startNextSeason() {
        List<String> news = new ArrayList<>();
        year++;
        for (Team team : teams) {
            team.resetSeason();
            for (Player p : team.squad()) {
                int drop = p.newSeason(random);
                if (team == userTeam && drop > 0) {
                    news.add(String.format("%s(%d세) 노쇠화로 능력 -%d → %d", p.name(), p.age(), drop, p.rating()));
                }
            }
            if (team != userTeam) {
                chooseBestFormation(team);
                applyStyleInstructions(team);
            } else if (team.isAutoLineup()) {
                team.autoPick();
            }
        }
        schedule = ScheduleGenerator.doubleRoundRobin(teams, random);
        userResults.clear();
        roundsPlayed = 0;
        plan.clear();
        pendingReview = null;
        today = dateOfRound(0).minusDays(7);
        motivate();
        return news;
    }

    private void motivate() {
        if (userTeam.managerStyle() == superleague.model.ManagerStyle.MOTIVATOR) {
            userTeam.changeMorale(10);
        }
    }

    public int roundOn(LocalDate date) {
        for (int i = 0; i < totalRounds(); i++) {
            if (dateOfRound(i).equals(date)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isMatchDay() {
        return !isFinished() && today.equals(dateOfRound(roundsPlayed));
    }

    public DayActivity activityOn(LocalDate date) {
        DayActivity set = plan.get(date);
        if (set != null) {
            return set;
        }
        if (roundOn(date.minusDays(1)) >= 0) {
            return DayActivity.RECOVERY;
        }
        if (roundOn(date.plusDays(1)) >= 0) {
            return DayActivity.LIGHT;
        }
        return date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY ? DayActivity.REST : DayActivity.TRAINING;
    }

    public void setActivity(LocalDate date, DayActivity activity) {
        if (!date.isBefore(today) && roundOn(date) < 0) {
            plan.put(date, activity);
        }
    }

    public List<String> advanceDay() {
        List<String> news = new ArrayList<>();
        if (isFinished() || isMatchDay()) {
            return news;
        }
        applyDay(userTeam, activityOn(today), news);
        today = today.plusDays(1);
        return news;
    }

    private int randomRound(double x) {
        int base = (int) Math.floor(x);
        return base + (random.nextDouble() < x - base ? 1 : 0);
    }

    private void applyDay(Team team, DayActivity activity, List<String> news) {
        TrainingIntensity intensity = team.intensity();
        TeamTraining focus = team.teamTraining();
        double condition;
        double training;
        double morale;
        double injury;
        switch (activity) {
            case TRAINING -> {
                condition = (intensity.conditionDelta() + focus.condition) / 5.0;
                training = (intensity.trainingDelta() + focus.training) / 4.0;
                morale = (intensity.moraleDelta() + focus.morale) / 4.0;
                injury = intensity.injuryChance() / 5;
            }
            case LIGHT -> {
                condition = 0.5;
                training = 0.1;
                morale = 0;
                injury = intensity.injuryChance() / 20;
            }
            case RECOVERY -> {
                condition = 1;
                training = 0;
                morale = 0;
                injury = 0;
            }
            default -> {
                condition = 2;
                training = -0.25;
                morale = 0.5;
                injury = 0;
            }
        }
        if (team.trainingBoostRounds() > 0 && training > 0) {
            training *= 1.5;
        }
        if (team.managerStyle() == superleague.model.ManagerStyle.SYSTEM_BUILDER && training > 0) {
            training *= 1.3;
        }
        team.changeTraining(randomRound(training));
        team.changeMorale(randomRound(morale));
        for (Player p : team.squad()) {
            p.changeCondition(randomRound(condition));
            if (injury > 0 && p.isAvailable() && random.nextDouble() < injury) {
                int rounds = 1 + random.nextInt(3);
                p.injure(rounds);
                news.add(String.format("%s 훈련 중 %s 선수가 부상을 입었습니다. (%d경기 결장)", formatDate(today), p.name(), rounds));
            }
        }
    }

    public static String formatDate(LocalDate d) {
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        return String.format("%d.%02d.%02d (%s)", d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                days[d.getDayOfWeek().getValue() - 1]);
    }

    public int totalRounds() {
        return schedule.size();
    }

    public boolean isFinished() {
        return roundsPlayed >= schedule.size();
    }

    public List<MatchResult> userResults() {
        return userResults;
    }

    public List<Fixture> userSchedule() {
        return schedule.stream()
                .map(round -> round.stream().filter(f -> f.involves(userTeam)).findFirst().orElseThrow())
                .toList();
    }

    public Fixture nextUserFixture() {
        if (isFinished()) {
            return null;
        }
        return schedule.get(roundsPlayed).stream()
                .filter(f -> f.involves(userTeam))
                .findFirst()
                .orElseThrow();
    }

    public RoundReport playNextRound() {
        return playNextRound(null);
    }

    public RoundReport playNextRound(Consumer<LiveMatch> watcher) {
        List<Fixture> fixtures = schedule.get(roundsPlayed);
        List<String> news = new ArrayList<>();
        while (!isMatchDay() && !isFinished()) {
            news.addAll(advanceDay());
        }

        for (Team team : teams) {
            if (team != userTeam) {
                chooseAiIntensity(team);
                applyTraining(team, news);
            }
        }
        for (Team team : teams) {
            if (team == userTeam && !team.isAutoLineup()) {
                news.addAll(team.validateLineup());
            } else {
                team.autoPick();
            }
        }
        for (Fixture f : fixtures) {
            if (f.home() != userTeam) {
                chooseAiTactic(f.home(), f.away());
            }
            if (f.away() != userTeam) {
                chooseAiTactic(f.away(), f.home());
            }
        }

        List<MatchResult> results = new ArrayList<>();
        MatchResult userResult = null;
        for (Fixture f : fixtures) {
            MatchResult r;
            if (watcher != null && f.involves(userTeam)) {
                LiveMatch live = engine.start(f);
                watcher.accept(live);
                r = live.finish();
            } else {
                r = engine.play(f);
            }
            results.add(r);
            f.home().recordResult(r.homeGoals(), r.awayGoals());
            f.away().recordResult(r.awayGoals(), r.homeGoals());
            applyMorale(f.home(), f.away(), r.homeGoals(), r.awayGoals());
            applyMorale(f.away(), f.home(), r.awayGoals(), r.homeGoals());
            if (f.involves(userTeam)) {
                userResult = r;
                userResults.add(r);
            }
        }

        for (Team team : teams) {
            String event = events.maybeTrigger(team);
            if (event != null && team == userTeam) {
                news.add(event);
            }
            List<String> grown = new ArrayList<>();
            for (Player p : team.squad()) {
                double boost = (team.trainingBoostRounds() > 0 ? 1.5 : 1.0)
                        * (team.managerStyle() == superleague.model.ManagerStyle.DEVELOPER ? 1.4 : 1.0);
                if (random.nextDouble() < p.growthChance(team.training()) * boost) {
                    p.grow();
                    grown.add(String.format("%s %d", p.name(), p.rating()));
                }
            }
            if (team == userTeam && !grown.isEmpty()) {
                news.add("성장: " + String.join(", ", grown));
            }
            List<String> trained = new ArrayList<>();
            for (Player p : team.squad()) {
                String done = p.individualTraining(random);
                if (done != null) {
                    trained.add(done);
                }
            }
            if (team == userTeam && !trained.isEmpty()) {
                news.add("개인 훈련: " + String.join(", ", trained));
            }
            team.squad().forEach(Player::passRound);
            team.decayMorale();
            team.passPerkRound();
        }

        today = dateOfRound(roundsPlayed).plusDays(1);
        roundsPlayed++;
        List<String> reactions = Reactions.of(userResult, userTeam, rankOf(userTeam), random);
        OwnerReview review = roundsPlayed % OWNER_INTERVAL == 0 ? ownerReview() : null;
        pendingReview = review != null && review.reward() ? review : null;
        return new RoundReport(roundsPlayed, results, userResult, news, reactions, review);
    }

    private OwnerReview ownerReview() {
        List<MatchResult> last = userResults.subList(userResults.size() - OWNER_INTERVAL, userResults.size());
        int points = 0;
        double expected = 0;
        List<String> lines = new ArrayList<>();
        java.util.Map<Player, Integer> goals = new java.util.HashMap<>();
        for (MatchResult r : last) {
            Team opp = r.fixture().opponentOf(userTeam);
            int gf = r.goalsFor(userTeam);
            int ga = r.goalsAgainst(userTeam);
            int pts = gf > ga ? 3 : gf == ga ? 1 : 0;
            points += pts;
            expected += OwnerReview.expectedPoints(opp.tier().ordinal() - userTeam.tier().ordinal());
            lines.add(String.format("vs %s (%s급)  %d-%d  %s", opp.name(), opp.tier().name(), gf, ga,
                    pts == 3 ? "승" : pts == 1 ? "무" : "패"));
            for (superleague.sim.MatchEvent e : r.events()) {
                if (e.type() == superleague.sim.MatchEvent.Type.GOAL && !e.ownGoal() && e.player() != null
                        && userTeam.squad().contains(e.player())) {
                    goals.merge(e.player(), 1, Integer::sum);
                }
            }
        }
        Player mvp = goals.entrySet().stream()
                .max(java.util.Map.Entry.<Player, Integer>comparingByValue()
                        .thenComparing(en -> en.getKey().rating()))
                .map(java.util.Map.Entry::getKey)
                .orElse(userTeam.starPlayer());
        double diff = points - expected;
        String grade = OwnerReview.grade(diff);
        String penalty = null;
        if (grade.equals("대만족")) {
            userTeam.changeMorale(10);
        } else if (grade.equals("실망")) {
            userTeam.changeMorale(-8);
            penalty = "구단주가 실망을 표했습니다. 선수단 분위기가 가라앉았습니다. (사기 -8)";
        }
        String headline = String.format("최근 %d경기 승점 %d (구단주 기대치 %.1f) → %s", OWNER_INTERVAL, points, expected, grade);
        return new OwnerReview(grade, headline, lines, points, expected,
                grade.equals("대만족") || grade.equals("만족"), mvp, penalty);
    }

    public String claimPerk(Perk perk) {
        OwnerReview review = pendingReview;
        if (review == null) {
            return null;
        }
        pendingReview = null;
        return perk.apply(userTeam, review.mvp());
    }

    private void applyTraining(Team team, List<String> news) {
        TrainingIntensity intensity = team.intensity();
        team.changeTraining(intensity.trainingDelta() + team.teamTraining().training);
        team.changeMorale(intensity.moraleDelta() + team.teamTraining().morale);
        for (Player p : team.squad()) {
            p.changeCondition(intensity.conditionDelta() + team.teamTraining().condition);
            if (p.isAvailable() && random.nextDouble() < intensity.injuryChance()) {
                int rounds = 1 + random.nextInt(4);
                p.injure(rounds);
                if (team == userTeam) {
                    news.add(String.format("훈련 중 %s 선수가 부상을 입었습니다. (%d라운드 결장)", p.name(), rounds));
                }
            }
        }
    }

    private void applyMorale(Team team, Team opponent, int scored, int conceded) {
        int delta = scored > conceded ? 8 : scored == conceded ? 0 : -8;
        if (team.tier().isHigherThan(opponent.tier())) {
            if (scored == conceded) {
                delta -= 4;
            } else if (scored < conceded) {
                delta -= 6;
            }
        } else if (opponent.tier().isHigherThan(team.tier())) {
            if (scored > conceded) {
                delta += 8;
            } else if (scored == conceded) {
                delta += 4;
            }
        }
        if (delta < 0 && team.managerStyle() == superleague.model.ManagerStyle.MOTIVATOR) {
            delta = (int) Math.round(delta * 0.6);
        }
        team.changeMorale(delta);
    }

    private static void chooseBestFormation(Team team) {
        Formation best = Formation.F4_4_2;
        double bestScore = -1;
        for (Formation f : Formation.values()) {
            if (f == Formation.CUSTOM) {
                continue;
            }
            team.setFormation(f);
            team.autoPick();
            if (team.lineupStrength() > bestScore + 0.1) {
                bestScore = team.lineupStrength();
                best = f;
            }
        }
        team.setFormation(best);
        team.autoPick();
    }

    private static void applyStyleInstructions(Team team) {
        switch (team.preferredTactic()) {
            case VERY_ATTACKING -> team.instructions().set(5, 4, 5, 4);
            case ATTACKING -> team.instructions().set(4, 4, 4, 4);
            case DEFENSIVE -> team.instructions().set(2, 2, 2, 3);
            case VERY_DEFENSIVE -> team.instructions().set(2, 2, 1, 2);
            default -> team.instructions().set(3, 3, 3, 3);
        }
    }

    private void chooseAiIntensity(Team team) {
        if (random.nextDouble() < 0.5) {
            team.setIntensity(TrainingIntensity.NORMAL);
        } else if (team.averageCondition() < 75 || team.morale() < -30) {
            team.setIntensity(TrainingIntensity.LIGHT);
        } else if (team.training() < team.trainingCap() - 10 && team.morale() > 10) {
            team.setIntensity(TrainingIntensity.INTENSE);
        } else {
            team.setIntensity(TrainingIntensity.NORMAL);
        }
    }

    private void chooseAiTactic(Team team, Team opponent) {
        if (random.nextDouble() < 0.6) {
            team.setTactic(team.preferredTactic());
        } else if (opponent.tier().isHigherThan(team.tier())) {
            team.setTactic(random.nextDouble() < 0.5 ? Tactic.DEFENSIVE : Tactic.BALANCED);
        } else if (team.tier().isHigherThan(opponent.tier())) {
            team.setTactic(random.nextDouble() < 0.5 ? Tactic.ATTACKING : Tactic.BALANCED);
        } else {
            team.setTactic(Tactic.BALANCED);
        }
    }

    public List<String> boardReview(Function<Team, Reward> userChoice) {
        List<String> messages = new ArrayList<>();
        for (Team team : teams) {
            Verdict verdict = Verdict.of(team.tier(), rankOf(team));
            if (verdict == Verdict.OUTSTANDING) {
                for (Reward reward : Reward.values()) {
                    String msg = reward.apply(team);
                    if (team == userTeam) {
                        messages.add(msg);
                    }
                }
            } else if (verdict == Verdict.MET) {
                Reward reward = team == userTeam
                        ? userChoice.apply(team)
                        : Reward.values()[random.nextInt(Reward.values().length)];
                String msg = reward.apply(team);
                if (team == userTeam) {
                    messages.add(msg);
                }
            } else if (verdict == Verdict.MISSED) {
                team.changeMorale(-10);
                if (team == userTeam) {
                    messages.add("운영진이 성적 부진에 경고를 보냈습니다. (사기 -10)");
                }
            }
        }
        return messages;
    }

    public List<Team> standings() {
        List<Team> sorted = new ArrayList<>(teams);
        sorted.sort(Comparator.comparingInt(Team::points).reversed()
                .thenComparing(Comparator.comparingInt(Team::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt(Team::goalsFor).reversed())
                .thenComparing(Team::name));
        return sorted;
    }

    public int rankOf(Team team) {
        return standings().indexOf(team) + 1;
    }
}
