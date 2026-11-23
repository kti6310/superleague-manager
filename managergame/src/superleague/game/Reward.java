package superleague.game;

import superleague.model.Team;

public enum Reward {
    FACILITY("구단 시설 향상", "시설 레벨 +1 (훈련 수준 상한 +8, 경기력 +1%)"),
    BONUS("선수단 보너스 지급", "사기 +25");

    private final String label;
    private final String effect;

    Reward(String label, String effect) {
        this.label = label;
        this.effect = effect;
    }

    public String label() {
        return label;
    }

    public String effect() {
        return effect;
    }

    public String apply(Team team) {
        if (this == FACILITY) {
            if (team.upgradeFacility()) {
                return String.format("%s: 시설 레벨이 %d(으)로 올랐습니다.", team.name(), team.facility());
            }
            team.changeMorale(15);
            return String.format("%s: 시설이 이미 최고 수준이라 대신 사기가 올랐습니다.", team.name());
        }
        team.changeMorale(25);
        return String.format("%s: 보너스 지급으로 사기가 크게 올랐습니다.", team.name());
    }
}
