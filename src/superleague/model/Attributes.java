package superleague.model;

import java.util.Map;
import java.util.Random;

public final class Attributes {
    public static final String[] NAMES = {"스피드", "슛", "패스", "드리블", "수비", "피지컬"};
    public static final int SHOOTING = 1;
    public static final int PASSING = 2;

    private static final Map<Role, int[]> PROFILE = Map.ofEntries(
            Map.entry(Role.GK, new int[]{-60, -120, -50, -80, -60, -20}),
            Map.entry(Role.CB, new int[]{-24, -60, -36, -34, 4, -4}),
            Map.entry(Role.LB, new int[]{6, -50, -14, -10, -10, -20}),
            Map.entry(Role.RB, new int[]{6, -50, -14, -10, -10, -20}),
            Map.entry(Role.LWB, new int[]{8, -44, -10, -6, -16, -22}),
            Map.entry(Role.RWB, new int[]{8, -44, -10, -6, -16, -22}),
            Map.entry(Role.DM, new int[]{-48, -20, -8, -12, -6, -10}),
            Map.entry(Role.CM, new int[]{-14, -28, 0, 6, -32, -32}),
            Map.entry(Role.AM, new int[]{-20, -6, -12, -4, -26, -16}),
            Map.entry(Role.LM, new int[]{4, -20, -10, -2, -50, -34}),
            Map.entry(Role.RM, new int[]{4, -20, -10, -2, -50, -34}),
            Map.entry(Role.ST, new int[]{-4, 4, -50, -22, -92, -6}),
            Map.entry(Role.CF, new int[]{-10, -2, -20, -6, -80, -20}),
            Map.entry(Role.LW, new int[]{10, -10, -18, 4, -110, -40}),
            Map.entry(Role.RW, new int[]{10, -10, -18, 4, -110, -40}));

    private Attributes() {
    }

    static int[] offsets(Player p) {
        int[] base = PROFILE.get(p.role());
        Random r = new Random(p.name().hashCode());
        int[] out = new int[NAMES.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = base[i] + (int) Math.round(r.nextGaussian() * 8);
        }
        out[0] -= (int) Math.round((p.height() - 182) * 0.5);
        out[5] += (int) Math.round((p.weight() - 76) * 0.8);
        return out;
    }

    static int clamp(int v) {
        return Math.max(30, Math.min(200, v));
    }

    public record Section(String title, String[] names, int[] values, int[] marks) {
    }

    private static final String[] TECHNICAL = {"개인기", "골 결정력", "드리블", "일대일 마크", "장거리 스로인", "중거리 슛",
            "코너킥", "크로스", "태클", "패스", "퍼스트 터치", "페널티킥", "프리킥", "헤더"};
    private static final String[] GOALKEEPING = {"공중볼 장악", "공중볼 처리", "의사소통", "괴팍함", "핸들링", "킥", "일대일",
            "펀칭 성향", "반사 신경", "러싱 아웃 성향", "스로잉", "패스", "퍼스트 터치"};
    private static final String[] MENTAL = {"대담성", "리더십", "수비 위치 선정", "승부욕", "시야", "예측력", "오프 더 볼",
            "적극성", "집중력", "천재성", "침착성", "팀워크", "판단력", "활동량"};
    private static final String[] PHYSICAL = {"가속도", "균형 감각", "몸싸움", "민첩성", "점프 거리", "주력", "지구력", "타고난 체력"};

    public static Section[] sections(Player p) {
        int[] a = p.attributes();
        int spd = a[0], sho = a[1], pas = a[2], dri = a[3], def = a[4], phy = a[5], ovr = p.rating();
        boolean gk = p.position() == Position.GK;
        Random r = new Random(p.name().hashCode() * 31L + 7);
        double[] tech = gk
                ? new double[]{ovr, ovr - 6, ovr - 10, ovr - 70, ovr, pas + 10, ovr, ovr - 40, ovr + 4, ovr - 30, pas, pas, dri}
                : new double[]{dri, sho, dri, def, phy * 0.6, sho, pas - 6, pas, def, pas, (dri + pas) / 2.0, sho,
                (sho + pas) / 2.0, phy * 0.45 + def * 0.3 + sho * 0.25 + (p.height() - 182) * 2.5 - 12};
        double[] mental = {(ovr + dri) / 2.0, ovr - 20 + (p.age() - 24) * 3, def, (ovr + phy) / 2.0, pas, (ovr + def) / 2.0,
                gk ? 30 : (sho + spd) / 2.0, (def + phy) / 2.0, ovr, (dri + pas) / 2.0, (sho + ovr) / 2.0, ovr,
                (pas + ovr) / 2.0, (phy + def) / 2.0};
        double[] physical = {spd, dri, phy, (spd + dri) / 2.0, phy + (p.height() - 180) * 2, spd, phy, phy};
        return new Section[]{
                section(gk ? "골키퍼 능력" : "기술적 능력", gk ? GOALKEEPING : TECHNICAL, tech, p, r),
                section("정신적 능력", MENTAL, mental, p, r),
                section("신체", PHYSICAL, physical, p, r)};
    }

    private static Section section(String title, String[] names, double[] base, Player p, Random r) {
        int[] values = new int[names.length];
        int[] marks = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = Math.max(1, Math.min(20, (int) Math.round((base[i] + r.nextGaussian() * 8) / 10.0)));
            marks[i] = mark(p.role(), names[i]);
        }
        return new Section(title, names, values, marks);
    }

    private static int mark(Role role, String name) {
        String[][] keys = switch (role) {
            case GK -> new String[][]{{"반사 신경", "핸들링", "일대일", "공중볼 장악"}, {"집중력", "판단력", "의사소통", "킥", "민첩성"}};
            case CB -> new String[][]{{"일대일 마크", "태클", "헤더", "수비 위치 선정", "점프 거리"}, {"집중력", "예측력", "몸싸움", "침착성", "주력"}};
            case LB, RB, LWB, RWB -> new String[][]{{"태클", "일대일 마크", "크로스", "활동량", "주력"}, {"수비 위치 선정", "집중력", "지구력", "가속도", "팀워크"}};
            case DM -> new String[][]{{"태클", "수비 위치 선정", "예측력", "팀워크", "패스"}, {"집중력", "판단력", "몸싸움", "지구력", "활동량"}};
            case CM -> new String[][]{{"패스", "시야", "판단력", "팀워크", "퍼스트 터치"}, {"활동량", "침착성", "예측력", "지구력", "태클"}};
            case AM -> new String[][]{{"패스", "시야", "천재성", "퍼스트 터치", "개인기"}, {"판단력", "오프 더 볼", "침착성", "민첩성", "드리블"}};
            case LM, RM, LW, RW -> new String[][]{{"드리블", "크로스", "개인기", "가속도", "주력", "오프 더 볼"}, {"퍼스트 터치", "패스", "판단력", "민첩성", "천재성"}};
            case ST, CF -> new String[][]{{"골 결정력", "퍼스트 터치", "오프 더 볼", "침착성", "가속도"}, {"드리블", "개인기", "판단력", "예측력", "주력", "균형 감각"}};
        };
        return java.util.Arrays.asList(keys[0]).contains(name) ? 2 : java.util.Arrays.asList(keys[1]).contains(name) ? 1 : 0;
    }

    public static String[][] profile(Player p) {
        int ovr = p.rating();
        int[] a = p.attributes();
        Random r = new Random(p.name().hashCode() * 17L + 3);
        String fame = ovr >= 185 ? "세계적으로 널리 알려짐" : ovr >= 172 ? "대륙에서 유명함" : ovr >= 158 ? "국내에서 유명함" : "지역에서 알려짐";
        String[] personalities = {"쉬지 않는 노력파", "전문가 정신", "모범적", "야심가", "결단력 있음", "균형 잡힌 성격", "냉철함", "열정적"};
        String personality = personalities[r.nextInt(personalities.length)];
        String grade = ovr >= 182 ? "엘리트" : ovr >= 168 ? "수준급" : ovr >= 150 ? "준수한" : "평범한";
        String media = (p.age() <= 21 && p.potential() - ovr >= 10 ? "촉망받는 " : grade + " ") + p.role().description();
        int keeper = p.position() == Position.GK ? Math.max(1, Math.min(20, ovr / 10)) : 1 + r.nextInt(3);
        String[] traitNames = {"폭발적인 스피드로 수비 뒷공간 침투", "먼 거리에서도 슈팅 시도", "킬러 패스를 자주 시도",
                "현란한 개인기를 자주 시도", "적극적으로 태클", "몸싸움으로 공 지켜내기"};
        int best = 0;
        int second = 1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > a[best]) {
                second = best;
                best = i;
            } else if (i != best && a[i] > a[second]) {
                second = i;
            }
        }
        String traits = p.position() == Position.GK ? "골문을 비우지 않음\n빠른 볼 배급"
                : traitNames[best] + "\n" + traitNames[second];
        return new String[][]{{"명성", fame}, {"잘 쓰는 발", p.foot().label()}, {"성격", personality},
                {"언론 설명", media}, {"골키퍼 등급", String.valueOf(keeper)}, {"선수 특성", traits}};
    }
}
