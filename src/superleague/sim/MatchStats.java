package superleague.sim;

import java.util.List;

public record MatchStats(int homePossession, int homeShots, int awayShots, int homeOnTarget, int awayOnTarget,
                         int homeCorners, int awayCorners, int homeYellows, int awayYellows) {

    public int awayPossession() {
        return 100 - homePossession;
    }

    static MatchStats of(List<MatchEvent> events, int homePossession, int homeCorners, int awayCorners) {
        int[] shots = new int[2];
        int[] onTarget = new int[2];
        int[] yellows = new int[2];
        for (MatchEvent e : events) {
            int s = e.side() == MatchEvent.Side.HOME ? 0 : 1;
            switch (e.type()) {
                case GOAL -> {
                    if (!e.ownGoal()) {
                        shots[s]++;
                        onTarget[s]++;
                    }
                }
                case SAVE, BIG_SAVE -> {
                    shots[s]++;
                    onTarget[s]++;
                }
                case MISS, BLOCK, WOODWORK -> shots[s]++;
                case YELLOW -> yellows[s]++;
                default -> {
                }
            }
        }
        return new MatchStats(homePossession, shots[0], shots[1], onTarget[0], onTarget[1],
                homeCorners, awayCorners, yellows[0], yellows[1]);
    }
}
