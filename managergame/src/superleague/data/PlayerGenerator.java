package superleague.data;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import superleague.model.Foot;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Role;
import superleague.model.Team;

public final class PlayerGenerator {
    private static final Map<String, String[][]> NAME_POOLS = Map.of(
            "잉글랜드", new String[][]{
                    {"잭", "해리", "올리버", "조지", "찰리", "토마스", "윌리엄", "제임스", "루크", "메이슨"},
                    {"스미스", "워커", "브라운", "테일러", "휴즈", "베일리", "콜린스", "모리스", "프라이스", "레이놀즈"}},
            "스페인", new String[][]{
                    {"알바로", "파블로", "하비에르", "마르코스", "다니엘", "세르히오", "이케르", "아드리안", "루벤", "가비"},
                    {"가르시아", "로페스", "마르티네스", "산체스", "페레스", "고메스", "모레노", "히메네스", "루이스", "알론소"}},
            "이탈리아", new String[][]{
                    {"마테오", "로렌초", "알레산드로", "프란체스코", "다비데", "리카르도", "페데리코", "니콜로", "안드레아", "지안루카"},
                    {"로시", "루소", "페라리", "에스포지토", "비안키", "로마노", "콜롬보", "리치", "마리노", "그레코"}},
            "독일", new String[][]{
                    {"루카스", "레온", "펠릭스", "요나스", "니클라스", "팀", "막시밀리안", "파울", "율리안", "케빈"},
                    {"뮐러", "슈미트", "슈나이더", "피셔", "베버", "마이어", "바그너", "베커", "호프만", "코흐"}},
            "프랑스", new String[][]{
                    {"위고", "루이", "가브리엘", "아르튀르", "쥘", "라파엘", "앙투안", "테오", "킬리안", "마티스"},
                    {"마르탱", "베르나르", "뒤부아", "토마", "로베르", "리샤르", "프티", "뒤랑", "르루아", "모로"}},
            "튀르키예", new String[][]{
                    {"유수프", "에미르", "메흐메트", "아르다", "케렘", "부라크", "오잔", "하칸", "알리", "제키"},
                    {"일마즈", "카야", "데미르", "샤힌", "첼리크", "이을드즈", "아이든", "외즈튀르크", "아르슬란", "도간"}});

    private static final List<String> COUNTRIES = List.of("잉글랜드", "스페인", "이탈리아", "독일", "프랑스", "튀르키예");

    private static final Position[] SQUAD_TEMPLATE = {
            Position.GK, Position.GK, Position.GK,
            Position.DF, Position.DF, Position.DF, Position.DF, Position.DF, Position.DF, Position.DF, Position.DF,
            Position.MF, Position.MF, Position.MF, Position.MF, Position.MF, Position.MF, Position.MF,
            Position.FW, Position.FW, Position.FW, Position.FW, Position.FW
    };

    public static final Map<Position, Integer> MINIMUM = Map.of(
            Position.GK, 2, Position.DF, 6, Position.MF, 6, Position.FW, 4);

    private final Random random;
    private final Set<String> usedNames = new HashSet<>();

    public PlayerGenerator(Random random) {
        this.random = random;
    }

    private static final Map<Position, Role[]> ROLE_CYCLE = Map.of(
            Position.GK, new Role[]{Role.GK},
            Position.DF, new Role[]{Role.CB, Role.CB, Role.LB, Role.RB, Role.CB, Role.LB, Role.RB, Role.CB},
            Position.MF, new Role[]{Role.DM, Role.CM, Role.AM, Role.CM, Role.LM, Role.RM, Role.CM},
            Position.FW, new Role[]{Role.ST, Role.LW, Role.RW, Role.ST, Role.CF});

    public void fillSquad(Team team) {
        for (int i = 0; i < SQUAD_TEMPLATE.length; i++) {
            team.addPlayer(create(team, SQUAD_TEMPLATE[i], i % 2 == 0));
        }
    }

    private Role nextRole(Team team, Position position) {
        Role[] cycle = ROLE_CYCLE.get(position);
        long have = team.squad().stream().filter(p -> p.position() == position).count();
        return cycle[(int) (have % cycle.length)];
    }

    public int topUp(Team team) {
        int added = 0;
        for (Position position : Position.values()) {
            long have = team.squad().stream().filter(p -> p.position() == position).count();
            for (long i = have; i < MINIMUM.get(position); i++) {
                team.addPlayer(create(team, position, false));
                added++;
            }
        }
        return added;
    }

    public void reserveName(String name) {
        usedNames.add(name);
    }

    private Player create(Team team, Position position, boolean regular) {
        int base = team.tier().baseStrength();
        int center = regular ? base + 4 : base - 6;
        int rating = clamp((int) Math.round(center + random.nextGaussian() * 6), 110, 190);
        int age = 18 + random.nextInt(17);
        int headroom = age <= 21 ? 12 + random.nextInt(20) : age <= 25 ? 4 + random.nextInt(14) : random.nextInt(6);
        Player p = new Player(uniqueName(team.country()), age, position, rating, Math.min(200, rating + headroom), true);
        int height = (int) Math.round(Player.defaultHeight(position) + random.nextGaussian() * 5);
        int weight = (int) Math.round(height - 105 + random.nextGaussian() * 4);
        double f = random.nextDouble();
        Foot foot = f < 0.70 ? Foot.RIGHT : f < 0.95 ? Foot.LEFT : Foot.BOTH;
        return p.withProfile(team.country(), height, weight, foot).withRole(nextRole(team, position));
    }

    private String uniqueName(String homeCountry) {
        while (true) {
            String country = random.nextDouble() < 0.6 ? homeCountry : COUNTRIES.get(random.nextInt(COUNTRIES.size()));
            String[][] pool = NAME_POOLS.get(country);
            String name = pool[0][random.nextInt(pool[0].length)] + " " + pool[1][random.nextInt(pool[1].length)];
            if (usedNames.add(name)) {
                return name;
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
