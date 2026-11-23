package superleague.game;

import java.util.ArrayList;
import java.util.List;
import superleague.model.Team;
import superleague.sim.MatchResult;

public final class Tie {
    public final Stage stage;
    public final Team first;
    public final Team second;
    private final List<MatchResult> legs = new ArrayList<>();
    private Team winner;
    private String note = "";
    private Shootout shootout;

    Tie(Stage stage, Team first, Team second) {
        this.stage = stage;
        this.first = first;
        this.second = second;
    }

    void add(MatchResult r) {
        legs.add(r);
    }

    boolean complete() {
        return legs.size() == stage.legs;
    }

    public int goals(Team team) {
        return legs.stream().mapToInt(r -> r.goalsFor(team)).sum();
    }

    public Team winner() {
        return winner;
    }

    public String note() {
        return note;
    }

    public int legsPlayed() {
        return legs.size();
    }

    public Shootout shootout() {
        return shootout;
    }

    void resolve(java.util.Random random) {
        int a = goals(first);
        int b = goals(second);
        if (a != b) {
            winner = a > b ? first : second;
            note = stage.legs > 1 ? String.format("합계 %d-%d", a, b) : "";
            return;
        }
        Team home = legs.get(legs.size() - 1).fixture().home();
        shootout = new Shootout(home, home == first ? second : first, random);
        winner = shootout.winner();
        note = (stage.legs > 1 ? String.format("합계 %d-%d, ", a, b) : "") + "승부차기 " + shootout.score();
    }

    public String summary() {
        String score = legs.isEmpty() ? "vs" : goals(first) + " - " + goals(second);
        String text = first.name() + "  " + score + "  " + second.name();
        if (winner != null) {
            text += "  → " + winner.name() + (note.isEmpty() ? "" : " (" + note + ")");
        }
        return text;
    }
}
