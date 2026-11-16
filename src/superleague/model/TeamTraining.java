package superleague.model;

public enum TeamTraining {
    BALANCED("균형", "균형 훈련", "기본기와 전술을 고르게 연습합니다.", 0, 0, 0, 0, 0, 0),

    BUILD_UP("공격", "빌드업 패스", "후방부터 짧은 패스로 전개하는 연습입니다.", 3, 0, -2, 0, 0, 0),
    WING_PLAY("공격", "측면 공격·크로스", "측면 돌파와 크로스, 문전 침투를 연습합니다.", 3, 0, -2, 0, 0, 0),
    COUNTER("공격", "역습 전개", "공을 뺏은 뒤 빠르게 전방으로 나가는 연습입니다.", 3, 1, -3, 0, 0, 0),
    FINISHING("공격", "마무리 슈팅", "박스 안 마무리와 중거리 슈팅을 집중 연습합니다.", 4, 0, -2, 0, 0, 0),

    PRESSING("수비", "전방 압박", "상대 진영에서 공을 뺏는 압박 타이밍을 맞춥니다.", 1, 3, -3, 1, 0, 0),
    DEFENSIVE_LINE("수비", "수비 라인 조직", "라인 간격과 오프사이드 트랩을 맞춥니다.", 0, 4, -2, 0, 0, 0),
    MARKING("수비", "대인 마크", "일대일 수비와 세컨드 볼 경합을 연습합니다.", 0, 3, -2, 0, 0, 0),

    SET_PIECE_ATTACK("세트피스", "세트피스 공격", "코너킥·프리킥 약속된 플레이를 연습합니다.", 2, 0, -1, 0, 0, 0),
    SET_PIECE_DEFENSE("세트피스", "세트피스 수비", "코너킥·프리킥 수비 위치와 마크를 연습합니다.", 0, 2, -1, 0, 0, 0),

    ENDURANCE("체력", "지구력", "오래 뛰는 체력을 기릅니다. 이번 주는 힘들지만 경기에서 덜 지칩니다.", 0, 0, -4, -3, 1, 0),
    SPEED_AGILITY("체력", "스피드·민첩성", "짧은 거리 스프린트와 방향 전환을 연습합니다.", 1, 1, -4, 0, 1, 0),

    RECOVERY("회복·분위기", "회복 훈련", "가볍게 몸을 풀며 쉽니다.", 0, 0, 6, 0, -2, 0),
    REST("회복·분위기", "완전 휴식", "훈련 없이 쉽니다. 몸과 마음은 회복되지만 훈련 수준이 크게 떨어집니다.", 0, 0, 10, 0, -4, 3),
    TEAM_BONDING("회복·분위기", "팀 빌딩", "단합 행사로 팀 분위기를 끌어올립니다.", 0, 0, 2, 0, -1, 5);

    public static final String[] CATEGORIES = {"균형", "공격", "수비", "세트피스", "체력", "회복·분위기"};

    public final String category;
    public final String label;
    public final String description;
    public final double attack;
    public final double defense;
    public final int condition;
    public final int fatigue;
    public final int training;
    public final int morale;

    TeamTraining(String category, String label, String description, double attack, double defense, int condition,
                 int fatigue, int training, int morale) {
        this.category = category;
        this.label = label;
        this.description = description;
        this.attack = attack;
        this.defense = defense;
        this.condition = condition;
        this.fatigue = fatigue;
        this.training = training;
        this.morale = morale;
    }

    public String effectText() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (attack != 0) {
            parts.add(String.format("다음 경기 득점 기대 %+.0f%%", attack));
        }
        if (defense != 0) {
            parts.add(String.format("실점 억제 %+.0f%%", defense));
        }
        if (fatigue != 0) {
            parts.add(String.format("경기 체력 소모 %+d", fatigue));
        }
        if (condition != 0) {
            parts.add(String.format("컨디션 %+d", condition));
        }
        if (training != 0) {
            parts.add(String.format("훈련 수준 %+d", training));
        }
        if (morale != 0) {
            parts.add(String.format("사기 %+d", morale));
        }
        return parts.isEmpty() ? "보정 없음" : String.join(" · ", parts);
    }

    @Override
    public String toString() {
        return label;
    }
}
