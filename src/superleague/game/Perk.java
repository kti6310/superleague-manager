package superleague.game;

import superleague.model.Player;
import superleague.model.Team;

public enum Perk {
    MORALE("선수단 보너스", "사기 +15 (즉시)"),
    TEAMWORK("팀 워크숍", "다음 3경기 팀워크 +3% (경기력 상승)"),
    TRAINING("훈련 장비 지원", "다음 3경기 동안 성장 확률 ×1.5 · 훈련 효과 ×1.5"),
    PLAYER("MVP 특별 포상", "최근 3경기 MVP 컨디션 100 · 능력 +2");

    public static final int ROUNDS = 3;

    public final String label;
    public final String effect;

    Perk(String label, String effect) {
        this.label = label;
        this.effect = effect;
    }

    public String apply(Team team, Player mvp) {
        return switch (this) {
            case MORALE -> {
                team.changeMorale(15);
                yield "구단주가 보너스를 지급했습니다. 선수단 분위기가 밝아졌습니다. (사기 +15)";
            }
            case TEAMWORK -> {
                team.grantTeamwork(ROUNDS);
                yield "팀 워크숍으로 호흡이 좋아졌습니다. (다음 3경기 팀워크 +3%)";
            }
            case TRAINING -> {
                team.grantTrainingBoost(ROUNDS);
                yield "새 훈련 장비가 들어왔습니다. (다음 3경기 훈련 성취도 ×1.5)";
            }
            case PLAYER -> {
                mvp.changeCondition(100);
                mvp.grow();
                yield String.format("%s 선수가 MVP 포상을 받았습니다. (컨디션 회복 · 능력 %d)", mvp.name(), mvp.rating());
            }
        };
    }
}
