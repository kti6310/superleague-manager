package superleague.model;

public enum Tier {
    S(160, 4),
    A(154, 8),
    B(148, 10),
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

    public String targetLabel() {
        return switch (this) {
            case S -> "4강 진출";
            case A -> "8강 진출";
            case B -> "플레이오프 진출";
            case C -> "조 7위 이내";
        };
    }

    public int targetRank() {
        return targetRank;
    }

    public boolean isHigherThan(Tier other) {
        return ordinal() < other.ordinal();
    }
}
