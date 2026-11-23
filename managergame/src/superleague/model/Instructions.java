package superleague.model;

public class Instructions {
    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int MID = 3;

    private int pressing = MID;
    private int tempo = MID;
    private int line = MID;
    private int width = MID;

    public int pressing() {
        return pressing;
    }

    public int tempo() {
        return tempo;
    }

    public int line() {
        return line;
    }

    public int width() {
        return width;
    }

    public void set(int pressing, int tempo, int line, int width) {
        this.pressing = clamp(pressing);
        this.tempo = clamp(tempo);
        this.line = clamp(line);
        this.width = clamp(width);
    }

    public void setPressing(int v) {
        pressing = clamp(v);
    }

    public void setTempo(int v) {
        tempo = clamp(v);
    }

    public void setLine(int v) {
        line = clamp(v);
    }

    public void setWidth(int v) {
        width = clamp(v);
    }

    public double attackFactor() {
        return 1 + 0.03 * (tempo - MID) + 0.02 * (width - MID) + 0.03 * (line - MID);
    }

    public double concedeFactor() {
        return 1 + 0.03 * (tempo - MID) + 0.04 * (line - MID) - 0.04 * (pressing - MID);
    }

    public double aerialWeight() {
        return 0.5 + 0.25 * (width - 1);
    }

    public int extraFatigue() {
        int press = pressing > MID ? 3 * (pressing - MID) : pressing - MID;
        return press + (tempo - MID);
    }

    private static int clamp(int v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    public enum Style {
        CUSTOM("직접 설정", 0, 0, 0, 0, ""),
        BALANCED("기본", 3, 3, 3, 3, "모든 지시를 보통으로"),
        POSSESSION("점유율 축구", 4, 2, 4, 4, "천천히 돌리며 높은 위치에서 공을 지킴"),
        GEGENPRESS("게겐프레싱", 5, 5, 5, 3, "높은 압박과 빠른 전개, 체력 소모 큼"),
        COUNTER("역습", 2, 5, 2, 3, "내려서서 막다가 빠르게 역습"),
        DIRECT("롱볼·측면", 3, 4, 3, 5, "넓게 벌려 크로스와 공중볼 공략"),
        PARK_THE_BUS("버스 세우기", 1, 1, 1, 2, "최대한 내려서서 실점 억제");

        public final String label;
        public final int pressing;
        public final int tempo;
        public final int line;
        public final int width;
        public final String description;

        Style(String label, int pressing, int tempo, int line, int width, String description) {
            this.label = label;
            this.pressing = pressing;
            this.tempo = tempo;
            this.line = line;
            this.width = width;
            this.description = description;
        }

        public static Style of(Instructions in) {
            for (Style s : values()) {
                if (s != CUSTOM && s.pressing == in.pressing() && s.tempo == in.tempo() && s.line == in.line()
                        && s.width == in.width()) {
                    return s;
                }
            }
            return CUSTOM;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
