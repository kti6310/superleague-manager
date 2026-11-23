package superleague.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import superleague.model.Player;
import superleague.model.Role;
import superleague.model.Team;

/**
 * Team kits (shirt colours) and shirt numbers for the match view. Display only: no game logic.
 * Every club has a preset kit; only the user's own club can be recoloured (chosen on the team-select screen).
 */
final class Kits {
    /** shirt = main colour, trim = stripes/outline, number = colour of the shirt number. */
    record Kit(Color shirt, Color trim, Color number, boolean striped) {
    }

    record Match(Kit home, Kit away, Kit homeKeeper, Kit awayKeeper) {
    }

    private static final Map<String, Kit> PRESET = new HashMap<>();
    private static String userTeamName;
    private static Kit userKit;

    static {
        put("로스 블랑코스", 0xF5F5F5, 0xC9A227, 0x1F2937, false);
        put("맨체스터 블루스", 0x6CABDD, 0xFFFFFF, 0x0F1B33, false);
        put("디 바이언", 0xDC052D, 0xFFFFFF, 0xFFFFFF, false);
        put("더 레즈", 0xC8102E, 0xF6EB61, 0xFFFFFF, false);
        put("르 파리지앵", 0x004170, 0xDA291C, 0xFFFFFF, false);
        put("런던 거너스", 0xEF0107, 0xFFFFFF, 0xFFFFFF, false);
        put("바르사", 0x004D98, 0xA50044, 0xFDD835, true);
        put("밀라노 네라주리", 0x0068A8, 0x111111, 0xFFFFFF, true);
        put("런던 블루스", 0x034694, 0xFFFFFF, 0xFFFFFF, false);
        put("알레띠", 0xCB3524, 0xFFFFFF, 0x1E3A8A, true);
        put("맨체스터 레드", 0xDA291C, 0x111111, 0xFFFFFF, false);
        put("비안코네리", 0x111111, 0xF5F5F5, 0xC9A227, true);
        put("밀라노 로소네리", 0xE30613, 0x111111, 0xFFFFFF, true);
        put("런던 스퍼스", 0xF7F7F7, 0x132257, 0x132257, false);
        put("디 보루센", 0xFDE100, 0x111111, 0x111111, false);
        put("라젠발", 0xF2F2F2, 0xDD0741, 0xDD0741, false);
        put("아슬란", 0xA90432, 0xFDB912, 0xFDB912, false);
        put("코모", 0x1E4FA0, 0xFFFFFF, 0xFFFFFF, false);
    }

    /** Colours offered for the user's club on the team-select screen. */
    static final int[] PALETTE = {
            0xDC2626, 0xF97316, 0xFACC15, 0x16A34A, 0x0D9488, 0x6CABDD,
            0x2563EB, 0x1E3A8A, 0x7C3AED, 0xDB2777, 0x111111, 0xF5F5F5};

    private Kits() {
    }

    private static void put(String team, int shirt, int trim, int number, boolean striped) {
        PRESET.put(team, new Kit(new Color(shirt), new Color(trim), new Color(number), striped));
    }

    static Kit preset(String teamName) {
        return PRESET.getOrDefault(teamName, new Kit(new Color(0x64748B), Color.WHITE, Color.WHITE, false));
    }

    /** Plain (unstriped) kit in the chosen colour with a readable number colour. */
    static Kit solid(Color shirt) {
        Color ink = luminance(shirt) > 0.6 ? new Color(0x111827) : Color.WHITE;
        return new Kit(shirt, ink, ink, false);
    }

    /** Remember the user's club colour (null = use the club's own preset). Only one club can be customised. */
    static void setUserKit(String teamName, Kit kit) {
        userTeamName = kit == null ? null : teamName;
        userKit = kit;
    }

    static Kit of(Team team) {
        if (userKit != null && team.name().equals(userTeamName)) {
            return userKit;
        }
        return preset(team.name());
    }

    /** Kits for one match; the away side switches to a change kit if the colours are too close. */
    static Match forMatch(Team home, Team away) {
        Kit h = of(home);
        Kit a = of(away);
        if (distance(h.shirt(), a.shirt()) < 120) {
            a = luminance(a.shirt()) > 0.5
                    ? new Kit(new Color(0x1F2937), a.shirt(), a.shirt(), false)
                    : new Kit(new Color(0xF8FAFC), a.shirt(), a.shirt(), false);
            if (distance(h.shirt(), a.shirt()) < 120) {
                a = new Kit(new Color(0x7C3AED), Color.WHITE, Color.WHITE, false);
            }
        }
        Kit hk = new Kit(new Color(0x84CC16), new Color(0x1A2E05), new Color(0x1A2E05), false);
        Kit ak = new Kit(new Color(0xF97316), new Color(0x431407), new Color(0x431407), false);
        if (distance(hk.shirt(), a.shirt()) < 120 || distance(hk.shirt(), h.shirt()) < 120) {
            hk = new Kit(new Color(0x22D3EE), new Color(0x083344), new Color(0x083344), false);
        }
        if (distance(ak.shirt(), h.shirt()) < 120 || distance(ak.shirt(), a.shirt()) < 120) {
            ak = new Kit(new Color(0xE879F9), new Color(0x3B0764), new Color(0x3B0764), false);
        }
        return new Match(h, a, hk, ak);
    }

    static double luminance(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }

    private static double distance(Color a, Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr * 0.3 + dg * dg * 0.59 + db * db * 0.11) * 1.6;
    }

    // ---- shirt numbers -------------------------------------------------------------------------------

    private static final Map<Team, Map<Player, Integer>> NUMBERS = new WeakHashMap<>();

    /** Classic numbers by position; the best player at each position gets the first free one. */
    private static int[] classic(Role role) {
        return switch (role) {
            case GK -> new int[]{1, 13, 25, 31};
            case CB -> new int[]{4, 5, 15, 24, 33};
            case LB, LWB -> new int[]{3, 12, 26};
            case RB, RWB -> new int[]{2, 22, 27};
            case DM -> new int[]{6, 16, 18};
            case CM -> new int[]{8, 14, 16, 18, 20};
            case AM -> new int[]{10, 21, 19};
            case LM, LW -> new int[]{11, 17, 23};
            case RM, RW -> new int[]{7, 17, 19};
            case ST, CF -> new int[]{9, 19, 29, 39};
        };
    }

    static int number(Team team, Player player) {
        Map<Player, Integer> map = NUMBERS.computeIfAbsent(team, t -> new HashMap<>());
        Integer n = map.get(player);
        if (n != null) {
            return n;
        }
        List<Player> squad = new ArrayList<>(team.squad());
        if (!squad.contains(player)) {
            squad.add(player);
        }
        squad.sort(Comparator.comparingInt(Player::rating).reversed());
        Set<Integer> used = new HashSet<>(map.values());
        for (Player p : squad) {
            if (map.containsKey(p)) {
                continue;
            }
            int chosen = -1;
            for (int c : classic(p.role())) {
                if (!used.contains(c)) {
                    chosen = c;
                    break;
                }
            }
            for (int c = 14; chosen < 0 && c < 100; c++) {
                if (!used.contains(c)) {
                    chosen = c;
                }
            }
            used.add(chosen);
            map.put(p, chosen);
        }
        return map.get(player);
    }
}
