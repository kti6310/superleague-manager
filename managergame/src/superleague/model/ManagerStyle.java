package superleague.model;

public enum ManagerStyle {
    STRATEGIST("전략가", "상대의 운영을 읽고 맞춤 대응을 짜는 감독", "경기 운영 상성에서 이기면 기대 득점 +40% (기본 +25%)"),
    TACTICIAN("지략가", "꾀와 변칙으로 강자를 넘어뜨리는 감독", "더 높은 등급 팀 상대로 득점 +7% · 실점 -7%"),
    SYSTEM_BUILDER("체계구축가", "훈련과 조직으로 팀의 뼈대를 세우는 감독", "훈련 수준 상승 ×1.3 · 팀워크 +1.5%"),
    MOTIVATOR("동기부여가", "라커룸을 하나로 묶는 리더형 감독", "시즌 시작 사기 +10 · 경기 후 사기 하락 40% 완화"),
    DEVELOPER("육성가", "유망주를 스타로 키워내는 감독", "선수 성장 확률 ×1.4"),
    PRAGMATIST("실리주의자", "지지 않는 축구로 승점을 쌓는 감독", "실점 -6% · 득점 -2%");

    public final String label;
    public final String motto;
    public final String effect;

    ManagerStyle(String label, String motto, String effect) {
        this.label = label;
        this.motto = motto;
        this.effect = effect;
    }
}
