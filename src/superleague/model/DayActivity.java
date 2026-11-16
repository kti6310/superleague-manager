package superleague.model;

public enum DayActivity {
    TRAINING("훈련", "이번 주 팀 훈련과 훈련 강도대로 정상 훈련합니다."),
    LIGHT("가벼운 훈련", "전술 확인과 세트피스 위주로 가볍게 몸을 풉니다. 컨디션 조금 회복, 부상 위험 낮음"),
    RECOVERY("회복", "마사지·수영 등으로 피로를 풉니다. 컨디션 +1, 부상 위험 없음"),
    REST("휴식", "훈련 없이 쉽니다. 컨디션 +2, 사기 조금 상승, 훈련 수준 조금 하락");

    public final String label;
    public final String description;

    DayActivity(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String toString() {
        return label;
    }
}
