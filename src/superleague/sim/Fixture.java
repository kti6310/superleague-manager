package superleague.sim;

import superleague.model.Team;

public record Fixture(int round, Team home, Team away, boolean neutral) {
    public Fixture(int round, Team home, Team away) {
        this(round, home, away, false);
    }

    public boolean involves(Team team) {
        return home == team || away == team;
    }

    public Team opponentOf(Team team) {
        return home == team ? away : home;
    }
}
