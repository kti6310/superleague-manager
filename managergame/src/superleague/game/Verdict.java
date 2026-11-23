package superleague.game;

import superleague.model.Tier;

public enum Verdict {
    OUTSTANDING("기대 이상", 2),
    MET("목표 달성", 1),
    NEAR("기대 수준", 0),
    MISSED("기대 미달", 0);

    private final String label;
    private final int rewardCount;

    Verdict(String label, int rewardCount) {
        this.label = label;
        this.rewardCount = rewardCount;
    }

    public String label() {
        return label;
    }

    public int rewardCount() {
        return rewardCount;
    }

    public static Verdict of(Tier tier, int rank) {
        int target = tier.targetRank();
        if (rank <= target - 3) {
            return OUTSTANDING;
        }
        if (rank <= target) {
            return MET;
        }
        if (rank <= target + 2) {
            return NEAR;
        }
        return MISSED;
    }
}
