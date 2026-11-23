package superleague.game;

public enum Stage {
    PLAYOFF("플레이오프", 2),
    QUARTER("8강", 2),
    SEMI("4강", 2),
    FINAL("결승", 1);

    public final String label;
    public final int legs;

    Stage(String label, int legs) {
        this.label = label;
        this.legs = legs;
    }

    int reachedOnWin() {
        return ordinal() + 2;
    }
}
