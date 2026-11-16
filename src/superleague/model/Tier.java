package superleague.model;

public enum Tier {
    S(160, 5),
    A(154, 8),
    B(148, 11),
    C(142, 14);

    private final int baseStrength;
    private final int targetRank;

    Tier(int baseStrength, int targetRank) {
        this.baseStrength = baseStrength;
        this.targetRank = targetRank;
    }

    public int baseStrength() {
        return baseStrength;
    }

    public int targetRank() {
        return targetRank;
    }

    public boolean isHigherThan(Tier other) {
        return ordinal() < other.ordinal();
    }
}
