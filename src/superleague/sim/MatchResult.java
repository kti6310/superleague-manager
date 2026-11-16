package superleague.sim;

import java.util.List;
import superleague.model.Team;

public record MatchResult(Fixture fixture, int homeGoals, int awayGoals,
                          List<String> homeScorers, List<String> awayScorers,
                          List<MatchEvent> events, MatchStats stats) {

    public int goalsFor(Team team) {
        return fixture.home() == team ? homeGoals : awayGoals;
    }

    public int goalsAgainst(Team team) {
        return fixture.home() == team ? awayGoals : homeGoals;
    }

    public String scoreLine() {
        return String.format("%s %d - %d %s", fixture.home().name(), homeGoals, awayGoals, fixture.away().name());
    }
}
