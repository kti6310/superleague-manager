package superleague.sim;

import superleague.model.Player;

public record MatchEvent(int minute, int extra, Side side, Type type, String text, int homeScore, int awayScore,
                         Player player, boolean ownGoal) {

    public enum Side {
        HOME, AWAY, NONE
    }

    public enum Type {
        KICKOFF, INFO, SAVE, BIG_SAVE, MISS, BLOCK, WOODWORK, GOAL, YELLOW, RED, SUB, HALFTIME, FULLTIME
    }

    public String clock() {
        return extra > 0 ? minute + "+" + extra + "'" : minute + "'";
    }

    public int order() {
        return order(minute, extra, type);
    }

    static int order(int minute, int extra, Type type) {
        return type == Type.HALFTIME ? 459 : minute * 10 + extra;
    }

    public boolean isKey() {
        return type == Type.GOAL || type == Type.WOODWORK || type == Type.BIG_SAVE || type == Type.RED;
    }
}
