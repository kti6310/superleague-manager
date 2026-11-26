package superleague.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import superleague.model.Formation;
import superleague.model.Instructions;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Tactic;
import superleague.model.Team;

public final class LiveMatch {
    public static final int MAX_SUBS = 5;
    public static final int MAX_WINDOWS = 3;
    private static final double FATIGUE_EFFECT = 0.005;
    private static final double IN_MATCH_DRAIN = 1.6;
    private static final double INJURY_CHANCE = 0.011;
    private static final int INJURED_DROP = 45;

    private final MatchEngine engine;
    private final Fixture fixture;
    private final Map<Team, Player[]> starters = new HashMap<>();
    private final Map<Team, Integer> subsUsed = new HashMap<>();
    private final Map<Team, Integer> windowsUsed = new HashMap<>();
    private final Map<Team, Team.Setup> startSetup = new HashMap<>();
    private final Map<Player, int[]> played = new HashMap<>();
    private final java.util.Random random;
    // Pending in-match moments (injuries, AI bench checks), handled when the clock reaches them.
    private final List<Moment> moments = new ArrayList<>();
    private final java.util.Set<Player> hurt = new java.util.HashSet<>();
    private Team manual;
    private List<MatchEvent> events;
    private int possession;
    private int homeCorners;
    private int awayCorners;
    private int stoppage;
    private boolean finished;

    LiveMatch(MatchEngine engine, Fixture fixture) {
        this.engine = engine;
        this.fixture = fixture;
        this.random = new java.util.Random(engine.nextSeed());
        for (Team t : List.of(fixture.home(), fixture.away())) {
            Player[] xi = new Player[11];
            for (int i = 0; i < 11; i++) {
                xi[i] = t.slotPlayer(i);
                if (xi[i] != null) {
                    played.put(xi[i], new int[]{0, 90});
                }
            }
            starters.put(t, xi);
            subsUsed.put(t, 0);
            windowsUsed.put(t, 0);
            startSetup.put(t, t.saveSetup());
            for (Player p : xi) {
                if (p != null && random.nextDouble() < INJURY_CHANCE) {
                    moments.add(new Moment(10 * (3 + random.nextInt(86)), t, p));
                }
            }
            moments.add(new Moment(10 * (56 + random.nextInt(8)), t, null));
            moments.add(new Moment(10 * (67 + random.nextInt(7)), t, null));
            moments.add(new Moment(10 * (77 + random.nextInt(6)), t, null));
        }
        moments.sort(java.util.Comparator.comparingInt(Moment::order));
        Commentary.Result r = engine.simulate(fixture, -1, 0, 0, 0, fitness(fixture.home(), 0), fitness(fixture.away(), 0));
        events = r.events();
        possession = r.possession();
        homeCorners = r.homeCorners();
        awayCorners = r.awayCorners();
        stoppage = r.stoppage();
    }

    public Fixture fixture() {
        return fixture;
    }

    public int subsLeft(Team team) {
        return MAX_SUBS - subsUsed.getOrDefault(team, MAX_SUBS);
    }

    public int windowsLeft(Team team) {
        return MAX_WINDOWS - windowsUsed.getOrDefault(team, MAX_WINDOWS);
    }

    public boolean onPitch(Player p) {
        int[] span = played.get(p);
        return span != null && span[1] == 90;
    }

    public int stamina(Team team, Player p, int clock) {
        int[] span = played.get(p);
        if (span == null) {
            return p.condition();
        }
        int minutes = Math.max(0, Math.min(clock, span[1]) - span[0]);
        return Math.max(0, (int) Math.round(p.condition() - fatigue(team, p) * minutes / 90.0));
    }

    // In-match drain: the opponent's pressing makes you run more.
    private double fatigue(Team team, Player p) {
        Team opp = team == fixture.home() ? fixture.away() : fixture.home();
        return MatchEngine.fatigueFor(team, p) * IN_MATCH_DRAIN
                * (1 + 0.04 * (opp.instructions().pressing() - Instructions.MID));
    }

    // Average in-match tiredness of the current XI over the remaining minutes lowers team power.
    double fitness(Team team, int from) {
        double drop = 0;
        int n = 0;
        for (int i = 0; i < 11; i++) {
            Player p = team.slotPlayer(i);
            int[] span = p == null ? null : played.get(p);
            if (span == null) {
                continue;
            }
            double midMinute = (Math.max(from, span[0]) + 90) / 2.0;
            drop += fatigue(team, p) * Math.max(0, midMinute - span[0]) / 90.0
                    + (hurt.contains(p) ? INJURED_DROP : 0);
            n++;
        }
        return n == 0 ? 1 : 1 - FATIGUE_EFFECT * drop / n;
    }

    public List<Player> bench(Team team) {
        List<Player> out = new ArrayList<>();
        for (Player p : team.squad()) {
            if (!played.containsKey(p) && p.injuredRounds() == 0) {
                out.add(p);
            }
        }
        return out;
    }

    public boolean canSubstitute(Team team, int slot, Player in) {
        if (finished || subsLeft(team) <= 0 || windowsLeft(team) <= 0 || slot < 0 || slot >= 11 || in == null) {
            return false;
        }
        if (played.containsKey(in) || in.injuredRounds() > 0 || !team.squad().contains(in)) {
            return false;
        }
        Formation.Slot s = team.slots().get(slot);
        return s.group() != Position.GK || in.position() == Position.GK;
    }

    public boolean substitute(Team team, int slot, Player in, int minute, int extra, int cut) {
        return substitute(team, Map.of(slot, in), minute, extra, cut);
    }

    public boolean substitute(Team team, Map<Integer, Player> changes, int minute, int extra, int cut) {
        advance(cut);
        return substitute(team, changes, Map.of(), minute, extra, cut);
    }

    private boolean substitute(Team team, Map<Integer, Player> changes, Map<Integer, String> reasons,
                               int minute, int extra, int cut) {
        if (changes.isEmpty() || changes.size() > subsLeft(team)
                || changes.values().stream().distinct().count() != changes.size()) {
            return false;
        }
        for (Map.Entry<Integer, Player> c : changes.entrySet()) {
            if (!canSubstitute(team, c.getKey(), c.getValue())) {
                return false;
            }
        }
        int clock = cut >= 450 && cut <= 459 ? 45 : Math.min(90, cut / 10);
        MatchEvent.Side side = sideOf(team);
        List<MatchEvent> added = new ArrayList<>();
        for (Map.Entry<Integer, Player> c : changes.entrySet()) {
            Player out = team.slotPlayer(c.getKey());
            Player in = c.getValue();
            if (out != null) {
                played.get(out)[1] = clock;
            }
            played.put(in, new int[]{clock, 90});
            team.setSlot(c.getKey(), in);
            String why = reasons.get(c.getKey());
            added.add(new MatchEvent(minute, extra, side, MatchEvent.Type.SUB,
                    String.format("교체: %s OUT, %s IN (%s)%s", out == null ? "-" : out.name(), in.name(), team.name(),
                            why == null ? "" : " · " + why),
                    0, 0, in, false));
        }
        subsUsed.merge(team, changes.size(), Integer::sum);
        windowsUsed.merge(team, 1, Integer::sum);
        resimulate(cut, added);
        return true;
    }

    public boolean changeTactic(Team team, Tactic tactic, Instructions.Style style, int minute, int extra, int cut) {
        if (finished) {
            return false;
        }
        advance(cut);
        team.setTactic(tactic);
        if (style != Instructions.Style.CUSTOM) {
            team.instructions().set(style.pressing, style.tempo, style.line, style.width);
        }
        String text = String.format("전술 변경: %s · %s (%s)", tactic.label(), Instructions.Style.of(team.instructions()).label,
                team.name());
        resimulate(cut, List.of(new MatchEvent(minute, extra, sideOf(team), MatchEvent.Type.SUB, text, 0, 0, null, false)));
        return true;
    }

    // Applies whatever the manager changed on the tactics board mid-match; new faces in the XI count as substitutions.
    public String reorganize(Team team, Team.Setup before, int minute, int extra, int cut) {
        if (finished) {
            return "경기가 이미 끝났습니다.";
        }
        advance(cut);
        List<Player> oldXi = java.util.Arrays.stream(before.lineup()).filter(java.util.Objects::nonNull).toList();
        List<Player> newXi = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            if (team.slotPlayer(i) != null) {
                newXi.add(team.slotPlayer(i));
            }
        }
        List<Player> in = newXi.stream().filter(p -> !oldXi.contains(p)).toList();
        List<Player> out = oldXi.stream().filter(p -> !newXi.contains(p)).toList();
        String error = null;
        if (newXi.size() < 11) {
            error = "선발 11명을 모두 채워야 합니다.";
        } else if (!in.isEmpty() && windowsLeft(team) <= 0) {
            error = "교체 기회 3번을 모두 썼습니다.";
        } else if (in.size() > subsLeft(team)) {
            error = "남은 교체 인원(" + subsLeft(team) + "명)보다 많이 바꿨습니다.";
        } else if (in.stream().anyMatch(played::containsKey)) {
            error = "이미 교체되어 나간 선수는 다시 들어올 수 없습니다.";
        }
        if (error != null) {
            team.restoreSetup(before);
            return error;
        }
        boolean shapeChanged = team.formation() != before.formation() || !team.slots().equals(slotsOf(before))
                || team.tactic() != before.tactic()
                || !java.util.Arrays.equals(before.lineup(), currentLineup(team));
        if (in.isEmpty() && !shapeChanged
                && team.instructions().pressing() == before.instructions()[0]
                && team.instructions().tempo() == before.instructions()[1]
                && team.instructions().line() == before.instructions()[2]
                && team.instructions().width() == before.instructions()[3]) {
            return null;
        }
        int clock = cut >= 450 && cut <= 459 ? 45 : Math.min(90, cut / 10);
        MatchEvent.Side side = sideOf(team);
        List<MatchEvent> added = new ArrayList<>();
        for (int k = 0; k < in.size(); k++) {
            Player o = out.get(k);
            Player n = in.get(k);
            played.get(o)[1] = clock;
            played.put(n, new int[]{clock, 90});
            added.add(new MatchEvent(minute, extra, side, MatchEvent.Type.SUB,
                    String.format("교체: %s OUT, %s IN (%s)", o.name(), n.name(), team.name()), 0, 0, n, false));
        }
        if (!in.isEmpty()) {
            subsUsed.merge(team, in.size(), Integer::sum);
            windowsUsed.merge(team, 1, Integer::sum);
        }
        added.add(new MatchEvent(minute, extra, side, MatchEvent.Type.SUB,
                String.format("전술 변경: %s · %s (%s)", team.formation().label(), team.tactic().label(), team.name()),
                0, 0, null, false));
        resimulate(cut, added);
        return null;
    }

    private static List<Formation.Slot> slotsOf(Team.Setup s) {
        return s.formation() == Formation.CUSTOM && s.customSlots() != null ? s.customSlots() : s.formation().slots();
    }

    private static Player[] currentLineup(Team team) {
        Player[] xi = new Player[11];
        for (int i = 0; i < 11; i++) {
            xi[i] = team.slotPlayer(i);
        }
        return xi;
    }

    private MatchEvent.Side sideOf(Team team) {
        return team == fixture.home() ? MatchEvent.Side.HOME : MatchEvent.Side.AWAY;
    }

    private void resimulate(int cut, List<MatchEvent> added) {
        List<MatchEvent> prefix = new ArrayList<>();
        for (MatchEvent e : events) {
            if (e.order() <= cut) {
                prefix.add(e);
            }
        }
        int h = prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1).homeScore();
        int a = prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1).awayScore();
        for (MatchEvent e : added) {
            prefix.add(new MatchEvent(e.minute(), e.extra(), e.side(), e.type(), e.text(), h, a, e.player(), false));
        }
        int from = cut < 0 ? 0 : cut >= 450 && cut <= 459 ? 45 : Math.min(90, cut / 10);
        Commentary.Result r = engine.simulate(fixture, cut, h, a, stoppage,
                fitness(fixture.home(), from), fitness(fixture.away(), from));
        double before = 1 - Commentary.remainingFraction(cut);
        possession = (int) Math.round(possession * before + r.possession() * (1 - before));
        homeCorners = (int) Math.round(homeCorners * before) + r.homeCorners();
        awayCorners = (int) Math.round(awayCorners * before) + r.awayCorners();
        prefix.addAll(r.events());
        events = prefix;
    }

    // The live dialog leaves this team to the manager; every other team picks its own subs.
    public void setManual(Team team) {
        manual = team;
    }

    public boolean isHurt(Player p) {
        return hurt.contains(p) && onPitch(p);
    }

    // Plays out every pending moment up to the given event order; returns the players of the manual team hurt meanwhile.
    public List<Player> advance(int upTo) {
        List<Player> newlyHurt = new ArrayList<>();
        while (!finished && !moments.isEmpty() && moments.get(0).order() <= upTo) {
            Moment m = moments.remove(0);
            if (m.player() != null) {
                if (injure(m) && m.team() == manual) {
                    newlyHurt.add(m.player());
                }
            } else if (m.team() != manual) {
                aiBench(m.team(), m.order());
            }
        }
        return newlyHurt;
    }

    private boolean injure(Moment m) {
        Player p = m.player();
        if (!onPitch(p) || sentOff(p, m.order())) {
            return false;
        }
        double roll = random.nextDouble();
        int out = roll < 0.6 ? 1 + random.nextInt(2) : roll < 0.9 ? 3 + random.nextInt(3) : 6 + random.nextInt(5);
        String level = out <= 2 ? "가벼운 부상" : out <= 5 ? "부상" : "큰 부상";
        p.injure(out + 1); // +1: the round-end tick happens right after this match
        hurt.add(p);
        int minute = m.order() / 10;
        resimulate(m.order(), List.of(new MatchEvent(minute, 0, sideOf(m.team()), MatchEvent.Type.SUB,
                String.format("%s: %s 쓰러짐 · %d경기 결장 예상", level, p.name(), out),
                0, 0, p, false)));
        if (m.team() != manual) {
            int slot = slotOf(m.team(), p);
            Player in = slot < 0 ? null : bestBench(m.team(), slot, List.of());
            if (in != null) {
                substitute(m.team(), Map.of(slot, in), Map.of(slot, "부상 교체"), minute, 0, m.order());
            }
        }
        return true;
    }

    private void aiBench(Team team, int cut) {
        if (subsLeft(team) <= 0 || windowsLeft(team) <= 0) {
            return;
        }
        int minute = cut / 10;
        int diff = scoreFor(team, cut);
        boolean last = windowsLeft(team) == 1 || minute >= 77;
        int limit = last ? subsLeft(team) : Math.min(2, subsLeft(team) - 1);
        Map<Integer, Player> changes = new java.util.LinkedHashMap<>();
        Map<Integer, String> reasons = new HashMap<>();
        Tactic before = team.tactic();

        if (diff < 0 || (diff > 0 && minute >= 70)) {
            Tactic t = diff < 0 ? (diff <= -2 ? Tactic.VERY_ATTACKING : Tactic.ATTACKING) : Tactic.DEFENSIVE;
            Position group = diff < 0 ? Position.FW : Position.DF;
            String why = diff < 0 ? "전술적 교체(공격 강화)" : "전술적 교체(리드 지키기)";
            team.setTactic(t);
            int slot = weakest(team, cut, group == Position.FW ? Position.FW : Position.MF, changes);
            Player in = slot < 0 ? null : bestBench(team, slot, changes.values(), group);
            if (in != null && changes.size() < limit) {
                changes.put(slot, in);
                reasons.put(slot, why);
            }
        }
        int threshold = 66 + 3 * windowsUsed.get(team);
        List<Integer> tired = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Player p = team.slotPlayer(i);
            if (p != null && team.slots().get(i).group() != Position.GK && !changes.containsKey(i)
                    && !sentOff(p, cut) && (hurt.contains(p) || stamina(team, p, minute) < threshold)) {
                tired.add(i);
            }
        }
        tired.sort(java.util.Comparator.comparingInt(i -> stamina(team, team.slotPlayer(i), minute)));
        for (int i : tired) {
            if (changes.size() >= limit) {
                break;
            }
            Player in = bestBench(team, i, changes.values());
            if (in != null && in.effectiveRating() * Formation.fit(in, team.slots().get(i))
                    >= 0.8 * team.slotPlayer(i).effectiveRating()) {
                changes.put(i, in);
                reasons.put(i, hurt.contains(team.slotPlayer(i)) ? "부상 교체" : "체력 저하");
            }
        }
        if (changes.isEmpty() && minute >= 60) {
            int slot = -1;
            for (int i = 0; i < 11; i++) {
                Player p = team.slotPlayer(i);
                if (p != null && team.slots().get(i).group() != Position.GK && !sentOff(p, cut)
                        && (slot < 0 || stamina(team, p, minute) < stamina(team, team.slotPlayer(slot), minute))) {
                    slot = i;
                }
            }
            Player in = slot < 0 ? null : bestBench(team, slot, List.of());
            if (in != null && in.effectiveRating() * Formation.fit(in, team.slots().get(slot))
                    >= 0.8 * team.slotPlayer(slot).effectiveRating()) {
                changes.put(slot, in);
                reasons.put(slot, "체력 안배");
            }
        }
        if (team.tactic() != before) {
            resimulate(cut, List.of(new MatchEvent(minute, 0, sideOf(team), MatchEvent.Type.SUB,
                    String.format("전술 변경: %s (%s)", team.tactic().label(), team.name()), 0, 0, null, false)));
        }
        if (!changes.isEmpty()) {
            substitute(team, changes, reasons, minute, 0, cut);
        }
    }

    private int weakest(Team team, int cut, Position group, Map<Integer, Player> taken) {
        int best = -1;
        double worst = Double.MAX_VALUE;
        for (int i = 0; i < 11; i++) {
            Player p = team.slotPlayer(i);
            if (p == null || team.slots().get(i).group() != group || taken.containsKey(i) || sentOff(p, cut)) {
                continue;
            }
            double v = p.effectiveRating() * stamina(team, p, cut / 10) / 100.0;
            if (v < worst) {
                worst = v;
                best = i;
            }
        }
        return best;
    }

    private Player bestBench(Team team, int slot, java.util.Collection<Player> taken) {
        return bestBench(team, slot, taken, null);
    }

    private Player bestBench(Team team, int slot, java.util.Collection<Player> taken, Position prefer) {
        Formation.Slot s = team.slots().get(slot);
        return bench(team).stream()
                .filter(p -> !taken.contains(p) && canSubstitute(team, slot, p) && Formation.eligible(p, s))
                .max(java.util.Comparator.comparingDouble((Player p) -> p.effectiveRating() * Formation.fit(p, s)
                        * (prefer != null && p.position() == prefer ? 1.15 : 1)))
                .orElse(null);
    }

    private int slotOf(Team team, Player p) {
        for (int i = 0; i < 11; i++) {
            if (team.slotPlayer(i) == p) {
                return i;
            }
        }
        return -1;
    }

    private boolean sentOff(Player p, int cut) {
        return events.stream().anyMatch(e -> e.type() == MatchEvent.Type.RED && e.player() == p && e.order() <= cut);
    }

    private int scoreFor(Team team, int cut) {
        int h = 0;
        int a = 0;
        for (MatchEvent e : events) {
            if (e.order() <= cut) {
                h = e.homeScore();
                a = e.awayScore();
            }
        }
        return team == fixture.home() ? h - a : a - h;
    }

    private record Moment(int order, Team team, Player player) {
    }

    public MatchResult result() {
        MatchEvent last = events.get(events.size() - 1);
        List<String> homeScorers = new ArrayList<>();
        List<String> awayScorers = new ArrayList<>();
        for (MatchEvent e : events) {
            if (e.type() == MatchEvent.Type.GOAL && e.player() != null) {
                String name = e.player().name() + (e.ownGoal() ? " (자책)" : "");
                (e.side() == MatchEvent.Side.HOME ? homeScorers : awayScorers).add(name);
            }
        }
        return new MatchResult(fixture, last.homeScore(), last.awayScore(), homeScorers, awayScorers,
                List.copyOf(events), MatchStats.of(events, possession, homeCorners, awayCorners));
    }

    public MatchResult finish() {
        if (!finished) {
            advance(Integer.MAX_VALUE);
        }
        MatchResult r = result();
        if (finished) {
            return r;
        }
        finished = true;
        for (MatchEvent e : events) {
            if (e.type() == MatchEvent.Type.GOAL && !e.ownGoal() && e.player() != null) {
                e.player().addGoal();
                if (e.assist() != null) {
                    e.assist().addAssist();
                }
            }
        }
        for (Team team : starters.keySet()) {
            int conceded = team == fixture.home() ? r.awayGoals() : r.homeGoals();
            if (conceded == 0) {
                team.squad().stream()
                        .filter(p -> p.position() == superleague.model.Position.GK && played.containsKey(p)
                                && played.get(p)[1] - played.get(p)[0] >= 60)
                        .forEach(Player::addCleanSheet);
            }
        }
        for (Map.Entry<Team, Player[]> entry : starters.entrySet()) {
            Team team = entry.getKey();
            for (Player p : team.squad()) {
                int[] span = played.get(p);
                if (span != null) {
                    double share = Math.max(0.2, (span[1] - span[0]) / 90.0);
                    p.addAppearance();
                    p.changeCondition(-(int) Math.round(MatchEngine.fatigueFor(team, p) * share));
                }
            }
            team.restoreSetup(startSetup.get(team));
        }
        return r;
    }
}
