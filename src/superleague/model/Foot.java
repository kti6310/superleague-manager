package superleague.model;

public enum Foot {
    LEFT("왼발"),
    RIGHT("오른발"),
    BOTH("양발");

    private final String label;

    Foot(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Foot parse(String s) {
        if (s == null) {
            return null;
        }
        String v = s.strip().toUpperCase();
        return switch (v) {
            case "L", "LEFT", "왼발", "왼", "좌" -> LEFT;
            case "R", "RIGHT", "오른발", "오른", "우" -> RIGHT;
            case "B", "BOTH", "양발", "양" -> BOTH;
            default -> null;
        };
    }
}
