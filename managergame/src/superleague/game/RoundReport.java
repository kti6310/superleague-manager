package superleague.game;

import java.util.List;
import superleague.sim.MatchResult;

public record RoundReport(int round, List<MatchResult> results, MatchResult userResult, List<String> news,
                          List<String> reactions, OwnerReview ownerReview, Shootout shootout) {
}
