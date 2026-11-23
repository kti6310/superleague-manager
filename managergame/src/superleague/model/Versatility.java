package superleague.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Versatility {
    private static final Map<String, Map<Role, Double>> TABLE = new HashMap<>();

    static {
        slot("GK", Role.GK, 1.0);
        slot("CB", Role.CB, 1.0, Role.DM, 0.86, Role.LB, 0.86, Role.RB, 0.86, Role.LWB, 0.80, Role.RWB, 0.80);
        slot("LB", Role.LB, 1.0, Role.LWB, 0.97, Role.RB, 0.90, Role.CB, 0.86, Role.RWB, 0.86, Role.LM, 0.84);
        slot("RB", Role.RB, 1.0, Role.RWB, 0.97, Role.LB, 0.90, Role.CB, 0.86, Role.LWB, 0.86, Role.RM, 0.84);
        slot("LWB", Role.LWB, 1.0, Role.LB, 0.97, Role.LM, 0.92, Role.RWB, 0.90, Role.RB, 0.86, Role.LW, 0.85);
        slot("RWB", Role.RWB, 1.0, Role.RB, 0.97, Role.RM, 0.92, Role.LWB, 0.90, Role.LB, 0.86, Role.RW, 0.85);
        slot("DM", Role.DM, 1.0, Role.CM, 0.95, Role.CB, 0.86, Role.AM, 0.84);
        slot("CM", Role.CM, 1.0, Role.DM, 0.95, Role.AM, 0.94, Role.LM, 0.88, Role.RM, 0.88);
        slot("AM", Role.AM, 1.0, Role.CM, 0.92, Role.CF, 0.92, Role.LM, 0.88, Role.RM, 0.88,
                Role.LW, 0.88, Role.RW, 0.88, Role.ST, 0.84);
        slot("LM", Role.LM, 1.0, Role.LW, 0.95, Role.RM, 0.92, Role.AM, 0.90, Role.LWB, 0.88, Role.CM, 0.88,
                Role.RW, 0.88, Role.LB, 0.82);
        slot("RM", Role.RM, 1.0, Role.RW, 0.95, Role.LM, 0.92, Role.AM, 0.90, Role.RWB, 0.88, Role.CM, 0.88,
                Role.LW, 0.88, Role.RB, 0.82);
        slot("LAM", Role.LM, 0.98, Role.LW, 0.97, Role.AM, 0.96, Role.RM, 0.92, Role.RW, 0.92, Role.CF, 0.86,
                Role.CM, 0.85);
        slot("RAM", Role.RM, 0.98, Role.RW, 0.97, Role.AM, 0.96, Role.LM, 0.92, Role.LW, 0.92, Role.CF, 0.86,
                Role.CM, 0.85);
        slot("LW", Role.LW, 1.0, Role.RW, 0.95, Role.LM, 0.94, Role.CF, 0.90, Role.AM, 0.88, Role.ST, 0.88,
                Role.RM, 0.88);
        slot("RW", Role.RW, 1.0, Role.LW, 0.95, Role.RM, 0.94, Role.CF, 0.90, Role.AM, 0.88, Role.ST, 0.88,
                Role.LM, 0.88);
        slot("ST", Role.ST, 1.0, Role.CF, 0.96, Role.LW, 0.88, Role.RW, 0.88, Role.AM, 0.82);
        slot("CF", Role.CF, 1.0, Role.ST, 0.97, Role.AM, 0.92, Role.LW, 0.86, Role.RW, 0.86);
        slot("LF", Role.LW, 1.0, Role.ST, 0.95, Role.CF, 0.92, Role.RW, 0.90, Role.LM, 0.86, Role.AM, 0.85);
        slot("RF", Role.RW, 1.0, Role.ST, 0.95, Role.CF, 0.92, Role.LW, 0.90, Role.RM, 0.86, Role.AM, 0.85);
    }

    private Versatility() {
    }

    private static void slot(String label, Object... pairs) {
        Map<Role, Double> m = new EnumMap<>(Role.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Role) pairs[i], (Double) pairs[i + 1]);
        }
        TABLE.put(label, m);
    }

    public static double of(Role role, String slotLabel) {
        Map<Role, Double> m = TABLE.get(slotLabel);
        if (m == null) {
            return 0;
        }
        return m.getOrDefault(role, 0.0);
    }

    public static boolean canPlay(Role role, String slotLabel) {
        return of(role, slotLabel) > 0;
    }
}
