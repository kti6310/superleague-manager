package superleague.model;

public enum Tactic {
    VERY_ATTACKING("매우 공격적", 1.28, 1.26, Family.ATTACK),
    ATTACKING("공격적", 1.15, 1.12, Family.ATTACK),
    BALANCED("밸런스", 1.00, 1.00, Family.BALANCE),
    DEFENSIVE("수비적", 0.85, 0.85, Family.DEFENSE),
    VERY_DEFENSIVE("매우 수비적", 0.72, 0.72, Family.DEFENSE);

    public enum Family {
        ATTACK("공격형"),
        BALANCE("밸런스형"),
        DEFENSE("수비형");

        private final String label;

        Family(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public Family beats() {
            return switch (this) {
                case ATTACK -> BALANCE;
                case BALANCE -> DEFENSE;
                case DEFENSE -> ATTACK;
            };
        }

        public Family beatenBy() {
            return switch (this) {
                case ATTACK -> DEFENSE;
                case BALANCE -> ATTACK;
                case DEFENSE -> BALANCE;
            };
        }
    }

    private final String label;
    private final double attackFactor;
    private final double concedeFactor;
    private final Family family;

    Tactic(String label, double attackFactor, double concedeFactor, Family family) {
        this.label = label;
        this.attackFactor = attackFactor;
        this.concedeFactor = concedeFactor;
        this.family = family;
    }

    public String label() {
        return label;
    }

    public double attackFactor() {
        return attackFactor;
    }

    public double concedeFactor() {
        return concedeFactor;
    }

    public Family family() {
        return family;
    }

    public String beatsLabel() {
        return family.beats().label();
    }

    public boolean counters(Tactic other) {
        return family.beats() == other.family;
    }
}
