package superleague.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum TacticalRole {
    GOALKEEPER("골키퍼", "골문을 지키는 데 집중합니다. 가장 안정적인 선택입니다.", 0, 0, 1, 1, 0, "GK"),
    SWEEPER_KEEPER("스위퍼 키퍼", "라인 뒤 공간을 넓게 커버하고 빌드업에 참여합니다. 공격 전개 +, 실점 위험 +.", 1, -1, 1, 1, 0, "GK"),

    CENTRE_BACK("중앙 수비수", "자리를 지키며 안정적으로 막습니다.", 0, 0, 1, 1, 0, "CB"),
    BALL_PLAYING_DEFENDER("볼 플레잉 디펜더", "후방에서 패스로 빌드업을 시작합니다. 공격 +, 수비 -.", 1, -1, 1, 1, 0, "CB"),
    STOPPER("스토퍼", "앞으로 나가 상대 공격수를 먼저 끊습니다. 수비 +, 체력 소모 +.", 0, 1, 1, 1, 2, "CB"),
    COVER("커버 수비수", "한 발 물러서 뒷공간을 커버합니다. 수비 +, 빌드업 -.", -1, 1, 1, 1, 0, "CB"),

    FULL_BACK("풀백", "공격과 수비를 균형 있게 합니다.", 0, 0, 1, 1, 0, "LB", "RB"),
    ATTACKING_FULL_BACK("공격적 풀백", "측면을 적극적으로 올라가 크로스를 올립니다. 공격 +, 수비 -, 체력 소모 +.", 1.5, -1.5, 1, 1.3, 2, "LB", "RB"),
    INVERTED_FULL_BACK("인버티드 풀백", "중앙으로 좁혀 미드필드 수적 우위를 만듭니다. 빌드업 +, 측면 수비 -.", 1, -1, 1, 1.1, 0, "LB", "RB"),
    DEFENSIVE_FULL_BACK("수비형 풀백", "오버래핑 없이 수비에 전념합니다. 수비 +, 공격 -.", -1, 1, 1, 0.8, 0, "LB", "RB"),

    WING_BACK("윙백", "측면 전체를 오르내리며 공수에 모두 가담합니다.", 0, 0, 1, 1.1, 0, "LWB", "RWB"),
    COMPLETE_WING_BACK("컴플리트 윙백", "사실상 윙어처럼 공격합니다. 공격 ++, 수비 --, 체력 소모 큼.", 2, -2, 1.1, 1.4, 3, "LWB", "RWB"),
    INVERTED_WING_BACK("인버티드 윙백", "안쪽으로 들어와 패스를 연결합니다. 빌드업 +, 측면 수비 -.", 1, -1, 1, 1.2, 1, "LWB", "RWB"),
    DEFENSIVE_WING_BACK("수비형 윙백", "5백처럼 내려서 수비합니다. 수비 +, 공격 -.", -1, 1, 1, 0.8, 0, "LWB", "RWB"),

    DEFENSIVE_MIDFIELDER("수비형 미드필더", "포백 앞에서 공수를 연결합니다.", 0, 0, 1, 1, 0, "DM"),
    ANCHOR("앵커", "포백 앞 자리를 지키며 거의 움직이지 않습니다. 수비 +, 공격 -.", -1.5, 1.5, 0.7, 0.8, 0, "DM"),
    DEEP_LYING_PLAYMAKER("딥라잉 플레이메이커", "후방에서 경기를 조율하는 패스 마스터입니다. 공격 +, 어시스트 ↑, 수비 -.", 1.5, -1.5, 0.9, 1.4, 0, "DM", "CM"),
    BALL_WINNING_MIDFIELDER("볼 위닝 미드필더", "적극적으로 압박해 공을 뺏습니다. 수비 +, 체력 소모 큼.", 0, 1.5, 0.9, 0.9, 3, "DM", "CM"),

    CENTRAL_MIDFIELDER("중앙 미드필더", "중원에서 공수 균형을 맞춥니다.", 0, 0, 1, 1, 0, "CM"),
    BOX_TO_BOX("박스 투 박스", "양쪽 페널티 박스를 오가며 공수 모두 가담합니다. 공격 +, 수비 +, 체력 소모 큼.", 1, 1, 1.2, 1.1, 4, "CM"),
    MEZZALA("메짤라", "하프스페이스로 침투하는 공격적인 중앙 미드필더입니다. 공격 +, 득점 ↑, 수비 -.", 1.5, -1.5, 1.3, 1.2, 1, "CM"),

    ATTACKING_MIDFIELDER("공격형 미드필더", "2선에서 패스와 슈팅으로 공격을 이끕니다.", 0, 0, 1, 1, 0, "AM"),
    ADVANCED_PLAYMAKER("어드밴스드 플레이메이커", "패스로 결정적인 기회를 만듭니다. 어시스트 ↑, 수비 -.", 1, -1, 0.9, 1.4, 0, "AM", "LAM", "RAM"),
    SHADOW_STRIKER("섀도 스트라이커", "최전방 뒤에서 박스로 침투해 득점을 노립니다. 득점 ↑, 수비 가담 -.", 1.5, -1.5, 1.5, 0.9, 1, "AM"),
    TREQUARTISTA("트레콰르티스타", "수비는 거의 하지 않고 창조적인 플레이에 집중합니다. 공격 ++, 수비 --.", 2, -2, 1.2, 1.5, 0, "AM", "CF"),

    WIDE_MIDFIELDER("측면 미드필더", "측면에서 공수 균형을 맞춥니다.", 0, 0, 1, 1, 0, "LM", "RM", "LAM", "RAM"),
    WINGER("윙어", "측면 끝까지 돌파해 크로스를 올립니다. 어시스트 ↑.", 0, 0, 0.9, 1.4, 0, "LM", "RM", "LAM", "RAM", "LW", "RW", "LF", "RF"),
    DEFENSIVE_WINGER("수비형 윙어", "측면 수비에 적극 가담합니다. 수비 +, 공격 -, 체력 소모 +.", -1, 1, 0.8, 0.9, 1, "LM", "RM", "LAM", "RAM", "LW", "RW"),
    WIDE_PLAYMAKER("와이드 플레이메이커", "측면에서 안쪽으로 좁혀 패스로 연결합니다. 빌드업 +, 측면 수비 -.", 1, -1, 0.9, 1.3, 0, "LM", "RM", "LAM", "RAM"),
    INSIDE_FORWARD("인사이드 포워드", "안쪽으로 파고들어 직접 슈팅합니다. 득점 ↑.", 1, -1, 1.4, 0.9, 1, "LW", "RW", "LF", "RF", "LAM", "RAM"),
    INVERTED_WINGER("인버티드 윙어", "안으로 접어 들어와 플레이메이킹을 합니다. 어시스트 ↑.", 1, -1, 1.1, 1.3, 1, "LW", "RW", "LM", "RM"),

    ADVANCED_FORWARD("침투형 포워드", "수비 뒷공간으로 계속 침투하는 공격수입니다. 득점 ↑.", 0, 0, 1.2, 1, 0, "ST", "CF", "LF", "RF"),
    POACHER("포처", "골문 앞에서 마무리만 노립니다. 득점 ↑↑, 연계·수비 가담 -.", 1, -1.5, 1.5, 0.6, 0, "ST", "CF"),
    SECOND_FORWARD("세컨드 포워드", "한 칸 내려와 연계하며 2선에서 침투합니다. 어시스트 ↑, 득점 ↓.", 0.5, -0.5, 0.8, 1.4, 1, "ST", "CF", "AM"),
    TARGET_MAN("타깃맨", "공중볼을 따내 동료에게 연결합니다. 공중볼·연계 +.", 1, -1, 1.1, 1.2, 0, "ST", "CF"),
    PRESSING_FORWARD("프레싱 포워드", "전방에서 수비수를 압박해 공을 뺏습니다. 수비 +, 체력 소모 큼.", -0.5, 1.5, 1, 1, 3, "ST", "CF", "LF", "RF"),
    FALSE_NINE("폴스 나인", "미드필드로 내려와 수적 우위를 만듭니다. 어시스트 ↑↑, 득점 ↓.", 1, -1, 0.7, 1.5, 0, "ST", "CF");

    public final String label;
    public final String description;
    public final double attack;
    public final double defense;
    public final double scorer;
    public final double assist;
    public final int fatigue;
    private final Set<String> slots;

    TacticalRole(String label, String description, double attack, double defense, double scorer, double assist,
                 int fatigue, String... slots) {
        this.label = label;
        this.description = description;
        this.attack = attack;
        this.defense = defense;
        this.scorer = scorer;
        this.assist = assist;
        this.fatigue = fatigue;
        this.slots = Set.of(slots);
    }

    public boolean fits(String slotLabel) {
        return slots.contains(slotLabel);
    }

    public static List<TacticalRole> forSlot(String slotLabel) {
        List<TacticalRole> out = new ArrayList<>();
        for (TacticalRole r : values()) {
            if (r.fits(slotLabel)) {
                out.add(r);
            }
        }
        return out;
    }

    public static TacticalRole defaultFor(String slotLabel) {
        List<TacticalRole> options = forSlot(slotLabel);
        for (TacticalRole r : options) {
            if (r.attack == 0 && r.defense == 0) {
                return r;
            }
        }
        return options.isEmpty() ? null : options.get(0);
    }

    public String effectText() {
        List<String> parts = new ArrayList<>();
        if (attack != 0) {
            parts.add(String.format("득점 기대 %+.1f%%", attack));
        }
        if (defense != 0) {
            parts.add(String.format("실점 억제 %+.1f%%", defense));
        }
        if (scorer != 1) {
            parts.add(String.format("골 확률 x%.1f", scorer));
        }
        if (assist != 1) {
            parts.add(String.format("도움 확률 x%.1f", assist));
        }
        if (fatigue != 0) {
            parts.add("체력 소모 +" + fatigue);
        }
        return parts.isEmpty() ? "보정 없음" : String.join(" · ", parts);
    }

    @Override
    public String toString() {
        return label;
    }
}
