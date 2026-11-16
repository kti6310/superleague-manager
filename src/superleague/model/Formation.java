package superleague.model;

import java.util.List;

public enum Formation {
    F4_4_2("4-4-2", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("LM", Position.MF, Side.LEFT, 0.12, 0.44),
            new Slot("CM", Position.MF, Side.CENTER, 0.37, 0.50),
            new Slot("CM", Position.MF, Side.CENTER, 0.63, 0.50),
            new Slot("RM", Position.MF, Side.RIGHT, 0.88, 0.44),
            new Slot("ST", Position.FW, Side.CENTER, 0.37, 0.18),
            new Slot("ST", Position.FW, Side.CENTER, 0.63, 0.18))),
    F4_3_3("4-3-3", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("CM", Position.MF, Side.CENTER, 0.25, 0.48),
            new Slot("CM", Position.MF, Side.CENTER, 0.50, 0.54),
            new Slot("CM", Position.MF, Side.CENTER, 0.75, 0.48),
            new Slot("LW", Position.FW, Side.LEFT, 0.15, 0.22),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.15),
            new Slot("RW", Position.FW, Side.RIGHT, 0.85, 0.22))),
    F4_2_3_1("4-2-3-1", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("DM", Position.MF, Side.CENTER, 0.37, 0.56),
            new Slot("DM", Position.MF, Side.CENTER, 0.63, 0.56),
            new Slot("LAM", Position.MF, Side.LEFT, 0.15, 0.33),
            new Slot("AM", Position.MF, Side.CENTER, 0.50, 0.35),
            new Slot("RAM", Position.MF, Side.RIGHT, 0.85, 0.33),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.14))),
    F3_5_2("3-5-2", List.of(
            Slot.gk(),
            new Slot("CB", Position.DF, Side.CENTER, 0.25, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.50, 0.78),
            new Slot("CB", Position.DF, Side.CENTER, 0.75, 0.76),
            new Slot("LWB", Position.DF, Side.LEFT, 0.08, 0.50),
            new Slot("CM", Position.MF, Side.CENTER, 0.30, 0.50),
            new Slot("CM", Position.MF, Side.CENTER, 0.50, 0.56),
            new Slot("CM", Position.MF, Side.CENTER, 0.70, 0.50),
            new Slot("RWB", Position.DF, Side.RIGHT, 0.92, 0.50),
            new Slot("ST", Position.FW, Side.CENTER, 0.37, 0.18),
            new Slot("ST", Position.FW, Side.CENTER, 0.63, 0.18))),
    F3_4_3("3-4-3", List.of(
            Slot.gk(),
            new Slot("CB", Position.DF, Side.CENTER, 0.25, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.50, 0.78),
            new Slot("CB", Position.DF, Side.CENTER, 0.75, 0.76),
            new Slot("LM", Position.MF, Side.LEFT, 0.10, 0.48),
            new Slot("CM", Position.MF, Side.CENTER, 0.37, 0.52),
            new Slot("CM", Position.MF, Side.CENTER, 0.63, 0.52),
            new Slot("RM", Position.MF, Side.RIGHT, 0.90, 0.48),
            new Slot("LW", Position.FW, Side.LEFT, 0.18, 0.20),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.15),
            new Slot("RW", Position.FW, Side.RIGHT, 0.82, 0.20))),
    F4_1_4_1("4-1-4-1", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("DM", Position.MF, Side.CENTER, 0.50, 0.58),
            new Slot("LM", Position.MF, Side.LEFT, 0.12, 0.40),
            new Slot("CM", Position.MF, Side.CENTER, 0.36, 0.44),
            new Slot("CM", Position.MF, Side.CENTER, 0.64, 0.44),
            new Slot("RM", Position.MF, Side.RIGHT, 0.88, 0.40),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.15))),
    F4_3_3_DM("4-3-3 (홀딩)", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("DM", Position.MF, Side.CENTER, 0.50, 0.58),
            new Slot("CM", Position.MF, Side.CENTER, 0.30, 0.44),
            new Slot("CM", Position.MF, Side.CENTER, 0.70, 0.44),
            new Slot("LW", Position.FW, Side.LEFT, 0.15, 0.22),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.15),
            new Slot("RW", Position.FW, Side.RIGHT, 0.85, 0.22))),
    F4_4_2_DIAMOND("4-4-2 다이아몬드", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("DM", Position.MF, Side.CENTER, 0.50, 0.60),
            new Slot("CM", Position.MF, Side.CENTER, 0.28, 0.47),
            new Slot("CM", Position.MF, Side.CENTER, 0.72, 0.47),
            new Slot("AM", Position.MF, Side.CENTER, 0.50, 0.34),
            new Slot("ST", Position.FW, Side.CENTER, 0.37, 0.17),
            new Slot("ST", Position.FW, Side.CENTER, 0.63, 0.17))),
    F4_4_1_1("4-4-1-1", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("LM", Position.MF, Side.LEFT, 0.12, 0.46),
            new Slot("CM", Position.MF, Side.CENTER, 0.37, 0.52),
            new Slot("CM", Position.MF, Side.CENTER, 0.63, 0.52),
            new Slot("RM", Position.MF, Side.RIGHT, 0.88, 0.46),
            new Slot("AM", Position.MF, Side.CENTER, 0.50, 0.32),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.15))),
    F4_2_2_2("4-2-2-2", List.of(
            Slot.gk(),
            new Slot("LB", Position.DF, Side.LEFT, 0.12, 0.70),
            new Slot("CB", Position.DF, Side.CENTER, 0.37, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.63, 0.76),
            new Slot("RB", Position.DF, Side.RIGHT, 0.88, 0.70),
            new Slot("DM", Position.MF, Side.CENTER, 0.37, 0.56),
            new Slot("DM", Position.MF, Side.CENTER, 0.63, 0.56),
            new Slot("LAM", Position.MF, Side.LEFT, 0.22, 0.34),
            new Slot("RAM", Position.MF, Side.RIGHT, 0.78, 0.34),
            new Slot("ST", Position.FW, Side.CENTER, 0.37, 0.15),
            new Slot("ST", Position.FW, Side.CENTER, 0.63, 0.15))),
    F3_4_2_1("3-4-2-1", List.of(
            Slot.gk(),
            new Slot("CB", Position.DF, Side.CENTER, 0.25, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.50, 0.78),
            new Slot("CB", Position.DF, Side.CENTER, 0.75, 0.76),
            new Slot("LWB", Position.DF, Side.LEFT, 0.08, 0.50),
            new Slot("CM", Position.MF, Side.CENTER, 0.37, 0.54),
            new Slot("CM", Position.MF, Side.CENTER, 0.63, 0.54),
            new Slot("RWB", Position.DF, Side.RIGHT, 0.92, 0.50),
            new Slot("AM", Position.MF, Side.CENTER, 0.33, 0.30),
            new Slot("AM", Position.MF, Side.CENTER, 0.67, 0.30),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.14))),
    F5_4_1("5-4-1", List.of(
            Slot.gk(),
            new Slot("LWB", Position.DF, Side.LEFT, 0.08, 0.64),
            new Slot("CB", Position.DF, Side.CENTER, 0.30, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.50, 0.78),
            new Slot("CB", Position.DF, Side.CENTER, 0.70, 0.76),
            new Slot("RWB", Position.DF, Side.RIGHT, 0.92, 0.64),
            new Slot("LM", Position.MF, Side.LEFT, 0.15, 0.44),
            new Slot("CM", Position.MF, Side.CENTER, 0.38, 0.48),
            new Slot("CM", Position.MF, Side.CENTER, 0.62, 0.48),
            new Slot("RM", Position.MF, Side.RIGHT, 0.85, 0.44),
            new Slot("ST", Position.FW, Side.CENTER, 0.50, 0.16))),
    F5_3_2("5-3-2", List.of(
            Slot.gk(),
            new Slot("LWB", Position.DF, Side.LEFT, 0.08, 0.62),
            new Slot("CB", Position.DF, Side.CENTER, 0.30, 0.76),
            new Slot("CB", Position.DF, Side.CENTER, 0.50, 0.78),
            new Slot("CB", Position.DF, Side.CENTER, 0.70, 0.76),
            new Slot("RWB", Position.DF, Side.RIGHT, 0.92, 0.62),
            new Slot("CM", Position.MF, Side.CENTER, 0.28, 0.46),
            new Slot("CM", Position.MF, Side.CENTER, 0.50, 0.50),
            new Slot("CM", Position.MF, Side.CENTER, 0.72, 0.46),
            new Slot("ST", Position.FW, Side.CENTER, 0.37, 0.18),
            new Slot("ST", Position.FW, Side.CENTER, 0.63, 0.18))),
    CUSTOM("사용자 지정", List.of());

    public static final List<Slot> DOTS = buildDots();

    private static List<Slot> buildDots() {
        double[] xs = {0.10, 0.30, 0.50, 0.70, 0.90};
        Object[][] rows = {
                {0.76, new String[]{"LB", "CB", "CB", "CB", "RB"}},
                {0.61, new String[]{"LWB", "DM", "DM", "DM", "RWB"}},
                {0.47, new String[]{"LM", "CM", "CM", "CM", "RM"}},
                {0.345, new String[]{"LW", "AM", "AM", "AM", "RW"}},
                {0.25, new String[]{null, "LF", "CF", "RF", null}},
                {0.15, new String[]{null, "ST", "ST", "ST", null}}};
        List<Slot> dots = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String[] labels = (String[]) row[1];
            for (int c = 0; c < xs.length; c++) {
                if (labels[c] == null) {
                    continue;
                }
                Role role = Role.forSlot(labels[c]);
                Side side = c == 0 ? Side.LEFT : c == xs.length - 1 ? Side.RIGHT : Side.CENTER;
                dots.add(new Slot(labels[c], role.group(), side, xs[c], (Double) row[0]));
            }
        }
        return List.copyOf(dots);
    }

    public enum Side {
        LEFT, CENTER, RIGHT
    }

    public record Slot(String label, Position group, Side side, double x, double y) {
        static Slot gk() {
            return new Slot("GK", Position.GK, Side.CENTER, 0.50, 0.92);
        }

        public Role role() {
            return Role.forSlot(label);
        }

        public String name() {
            if (!label.equals("CB") && !label.equals("DM") && !label.equals("CM") && !label.equals("ST")) {
                return label;
            }
            String base = label.equals("ST") ? "S" : label;
            return x < 0.45 ? "L" + base : x > 0.55 ? "R" + base : label;
        }
    }

    private final String label;
    private final List<Slot> slots;

    Formation(String label, List<Slot> slots) {
        this.label = label;
        this.slots = slots;
    }

    public String label() {
        return label;
    }

    public List<Slot> slots() {
        return slots;
    }

    @Override
    public String toString() {
        return label;
    }

    public static double positionFit(Position player, Position slot) {
        if (player == slot) {
            return 1.0;
        }
        if (player == Position.GK || slot == Position.GK) {
            return 0.5;
        }
        boolean dfMf = (player == Position.DF && slot == Position.MF) || (player == Position.MF && slot == Position.DF);
        boolean mfFw = (player == Position.MF && slot == Position.FW) || (player == Position.FW && slot == Position.MF);
        if (dfMf) {
            return 0.90;
        }
        if (mfFw) {
            return 0.93;
        }
        return 0.80;
    }

    public static double footFit(Foot foot, Slot slot) {
        if (slot.side() == Side.CENTER || foot == Foot.BOTH) {
            return 1.0;
        }
        boolean natural = (slot.side() == Side.LEFT && foot == Foot.LEFT) || (slot.side() == Side.RIGHT && foot == Foot.RIGHT);
        boolean attackingWide = slot.group() == Position.FW || slot.label().equals("LAM") || slot.label().equals("RAM");
        if (natural || attackingWide) {
            return 1.0;
        }
        return 0.95;
    }

    public static final double SECONDARY_FIT = 0.97;

    public static double roleValue(Player p, String label) {
        double v = Versatility.of(p.role(), label);
        for (Role r : p.secondary()) {
            v = Math.max(v, Versatility.of(r, label) * SECONDARY_FIT);
        }
        return v;
    }

    public static boolean eligible(Player p, Slot slot) {
        return roleValue(p, slot.label()) > 0;
    }

    public static double roleFit(Player p, Slot slot) {
        double v = roleValue(p, slot.label());
        if (v > 0) {
            return v;
        }
        return positionFit(p.position(), slot.group()) * 0.75;
    }

    public static double fit(Player p, Slot slot) {
        return roleFit(p, slot) * footFit(p.foot(), slot);
    }
}
