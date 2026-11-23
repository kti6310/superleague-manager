package superleague.model;

public enum Position {
    GK("GK"),
    DF("DF"),
    MF("MF"),
    FW("FW");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
