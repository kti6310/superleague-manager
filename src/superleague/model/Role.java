package superleague.model;

public enum Role {
    GK(Position.GK, "골키퍼"),
    CB(Position.DF, "센터백"),
    LB(Position.DF, "왼쪽 풀백"),
    RB(Position.DF, "오른쪽 풀백"),
    LWB(Position.DF, "왼쪽 윙백"),
    RWB(Position.DF, "오른쪽 윙백"),
    DM(Position.MF, "수비형 미드필더"),
    CM(Position.MF, "중앙 미드필더"),
    AM(Position.MF, "공격형 미드필더"),
    LM(Position.MF, "왼쪽 미드필더"),
    RM(Position.MF, "오른쪽 미드필더"),
    ST(Position.FW, "스트라이커"),
    CF(Position.FW, "센터 포워드"),
    LW(Position.FW, "왼쪽 윙어"),
    RW(Position.FW, "오른쪽 윙어");

    private final Position group;
    private final String description;

    Role(Position group, String description) {
        this.group = group;
        this.description = description;
    }

    public Position group() {
        return group;
    }

    public String description() {
        return description;
    }

    public static Role defaultFor(Position group) {
        return switch (group) {
            case GK -> GK;
            case DF -> CB;
            case MF -> CM;
            case FW -> ST;
        };
    }

    public static Role forSlot(String label) {
        return switch (label) {
            case "LAM" -> LM;
            case "RAM" -> RM;
            case "LF" -> LW;
            case "RF" -> RW;
            default -> valueOf(label);
        };
    }

    public static Role parse(String s) {
        if (s == null) {
            return null;
        }
        String v = s.strip().toUpperCase();
        return switch (v) {
            case "GK" -> GK;
            case "CB" -> CB;
            case "LB" -> LB;
            case "RB" -> RB;
            case "LWB" -> LWB;
            case "RWB" -> RWB;
            case "DM", "CDM" -> DM;
            case "CM" -> CM;
            case "AM", "CAM" -> AM;
            case "LM" -> LM;
            case "RM" -> RM;
            case "ST" -> ST;
            case "CF" -> CF;
            case "LW" -> LW;
            case "RW" -> RW;
            default -> null;
        };
    }
}
