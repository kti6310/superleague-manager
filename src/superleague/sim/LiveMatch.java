package superleague.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import superleague.model.Formation;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Team;

public final class LiveMatch {
    public static final int MAX_SUBS = 5;

    private final MatchEngine engine;
    private final Fixture fixture;
    private final Map<Team, Player[]> starters = new HashMap<>();
    private final Map<Team, Integer> subsUsed = new HashMap<>();
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
        }
        Commentary.Result r = engine.simulate(fixture, -1, 0, 0, 0);
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
        if (finished || subsLeft(team) <= 0 || slot < 0 || slot >= 11 || in == null) {
            return false;
        }
        if (played.containsKey(in) || in.injuredRounds() > 0 || !team.squad().contains(in)) {
            return false;
        }
        Formation.Slot s = team.slots().get(slot);
        return s.group() != Position.GK || in.position() == Position.GK;
    }

    public boolean substitute(Team team, int slot, Player in, int minute, int extra, int cut) {
        if (!canSubstitute(team, slot, in)) {
            return false;
        }
        Player out = team.slotPlayer(slot);
        int clock = cut >= 450 && cut <= 459 ? 45 : Math.min(90, cut / 10);
        if (out != null) {
            played.get(out)[1] = clock;
        }
        played.put(in, new int[]{clock, 90});
        team.setSlot(slot, in);
        subsUsed.merge(team, 1, Integer::sum);

        List<MatchEvent> prefix = new ArrayList<>();
        for (MatchEvent e : events) {
            if (e.order() <= cut) {
                prefix.add(e);
            }
        }
        int h = prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1).homeScore();
        int a = prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1).awayScore();
        MatchEvent.Side side = team == fixture.home() ? MatchEvent.Side.HOME : MatchEvent.Side.AWAY;
        String text = String.format("교체: %s OUT, %s IN (%s)", out == null ? "-" : out.name(), in.name(), team.name());
        prefix.add(new MatchEvent(minute, extra, side, MatchEvent.Type.SUB, text, h, a, in, false));

        Commentary.Result r = engine.simulate(fixture, cut, h, a, stoppage);
        double before = 1 - Commentary.remainingFraction(cut);
        possession = (int) Math.round(possession * before + r.possession() * (1 - before));
        homeCorners = (int) Math.round(homeCorners * before) + r.homeCorners();
        awayCorners = (int) Math.round(awayCorners * before) + r.awayCorners();
        prefix.addAll(r.events());
        events = prefix;
        return true;
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
        }
        return r;
    }
}
