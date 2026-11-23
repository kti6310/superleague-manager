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
    private static final double FATIGUE_EFFECT = 0.006;

    private final MatchEngine engine;
    private final Fixture fixture;
    private final Map<Team, Player[]> starters = new HashMap<>();
    private final Map<Team, Integer> subsUsed = new HashMap<>();
    private final Map<Team, Integer> windowsUsed = new HashMap<>();
    private final Map<Team, Tactic> startTactic = new HashMap<>();
    private final Map<Team, int[]> startInstructions = new HashMap<>();
    private final Map<Player, int[]> played = new HashMap<>();
    private List<MatchEvent> events;
    private int possession;
    private int homeCorners;
    private int awayCorners;
    private int stoppage;
    private boolean finished;

    LiveMatch(MatchEngine engine, Fixture fixture) {
        this.engine = engine;
        this.fixture = fixture;
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
            startTactic.put(t, t.tactic());
            Instructions in = t.instructions();
            startInstructions.put(t, new int[]{in.pressing(), in.tempo(), in.line(), in.width()});
        }
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
        return Math.max(0, (int) Math.round(p.condition() - MatchEngine.fatigueFor(team, p) * minutes / 90.0));
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
            drop += MatchEngine.fatigueFor(team, p) * Math.max(0, midMinute - span[0]) / 90.0;
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
            added.add(new MatchEvent(minute, extra, side, MatchEvent.Type.SUB,
                    String.format("교체: %s OUT, %s IN (%s)", out == null ? "-" : out.name(), in.name(), team.name()),
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
        team.setTactic(tactic);
        if (style != Instructions.Style.CUSTOM) {
            team.instructions().set(style.pressing, style.tempo, style.line, style.width);
        }
        String text = String.format("전술 변경: %s · %s (%s)", tactic.label(), Instructions.Style.of(team.instructions()).label,
                team.name());
        resimulate(cut, List.of(new MatchEvent(minute, extra, sideOf(team), MatchEvent.Type.SUB, text, 0, 0, null, false)));
        return true;
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
        MatchResult r = result();
        if (finished) {
            return r;
        }
        finished = true;
        for (MatchEvent e : events) {
            if (e.type() == MatchEvent.Type.GOAL && !e.ownGoal() && e.player() != null) {
                e.player().addGoal();
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
            Player[] xi = entry.getValue();
            for (int i = 0; i < xi.length; i++) {
                team.setSlot(i, xi[i]);
            }
            team.setTactic(startTactic.get(team));
            int[] in = startInstructions.get(team);
            team.instructions().set(in[0], in[1], in[2], in[3]);
        }
        return r;
    }
}
