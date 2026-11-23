package superleague.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import superleague.model.Attributes;
import superleague.model.Foot;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Role;
import superleague.model.Team;

public final class SquadLoader {
    public static final Path DEFAULT_PATH = Path.of("data", "players.csv");
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 200;

    private SquadLoader() {
    }

    public record Result(Path path, boolean fileFound, int loadedPlayers, int loadedTeams,
                         int generatedTeams, int toppedUp, List<String> warnings) {

        public String summary() {
            if (!fileFound) {
                return "선수 데이터 파일이 없어 모든 선수를 자동 생성했습니다.";
            }
            String text = String.format("data/players.csv에서 %d개 구단 선수 %d명을 불러왔습니다.",
                    loadedTeams, loadedPlayers);
            if (generatedTeams > 0) {
                text += String.format(" 선수가 없는 %d개 구단은 자동 생성했습니다.", generatedTeams);
            }
            if (toppedUp > 0) {
                text += String.format(" 포지션 부족분 %d명도 자동으로 채웠습니다.", toppedUp);
            }
            return text;
        }
    }

    public static Path resolve() {
        Path direct = DEFAULT_PATH.toAbsolutePath();
        if (Files.exists(direct)) {
            return direct;
        }
        Path nested = Path.of("superleague").resolve(DEFAULT_PATH).toAbsolutePath();
        if (Files.exists(nested)) {
            return nested;
        }
        Path browser = Path.of("/app").resolve(DEFAULT_PATH);
        if (Files.exists(browser)) {
            return browser;
        }
        return direct;
    }

    public static Result populate(List<Team> teams, Random random) {
        Path path = resolve();
        PlayerGenerator generator = new PlayerGenerator(random);
        List<String> warnings = new ArrayList<>();

        if (!Files.exists(path)) {
            teams.forEach(generator::fillSquad);
            return new Result(path, false, 0, 0, teams.size(), 0, warnings);
        }

        Map<String, Team> byName = new LinkedHashMap<>();
        for (Team t : teams) {
            byName.put(normalize(t.name()), t);
        }

        List<String> lines;
        try {
            lines = readLines(path);
        } catch (IOException e) {
            warnings.add("파일을 읽지 못했습니다: " + e.getMessage());
            teams.forEach(generator::fillSquad);
            return new Result(path, true, 0, 0, teams.size(), 0, warnings);
        }

        int loaded = 0;
        int lineNo = 0;
        boolean headerSkipped = false;
        for (String raw : lines) {
            lineNo++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> cols = splitCsv(line);
            if (!headerSkipped && !cols.isEmpty() && isHeader(cols.get(0))) {
                headerSkipped = true;
                continue;
            }
            headerSkipped = true;
            if (cols.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (cols.size() < 6) {
                warnings.add(String.format("%d행: 칸이 %d개뿐입니다. (구단,이름,포지션,나이,현재,최대 6칸 필요)", lineNo, cols.size()));
                continue;
            }
            Team team = byName.get(normalize(cols.get(0)));
            if (team == null) {
                warnings.add(String.format("%d행: '%s'은(는) 없는 구단 이름입니다.", lineNo, cols.get(0)));
                continue;
            }
            String name = cols.get(1).strip();
            if (name.isEmpty()) {
                warnings.add(String.format("%d행: 이름이 비어 있습니다.", lineNo));
                continue;
            }
            Role role = Role.parse(cols.get(2));
            Position position = role != null ? role.group() : parsePosition(cols.get(2));
            if (position == null) {
                warnings.add(String.format("%d행: 포지션 '%s'을(를) 알 수 없습니다. (GK, CB, LB, RB, LWB, RWB, DM, CM, AM, LM, RM, ST, CF, LW, RW 또는 GK/DF/MF/FW)", lineNo, cols.get(2)));
                continue;
            }
            Integer age = parseInt(cols.get(3));
            Integer current = parseInt(cols.get(4));
            Integer potential = parseInt(cols.get(5));
            if (age == null || current == null || potential == null) {
                warnings.add(String.format("%d행: 나이·현재·최대 능력은 숫자여야 합니다.", lineNo));
                continue;
            }
            if (current < MIN_RATING || current > MAX_RATING || potential < MIN_RATING || potential > MAX_RATING) {
                warnings.add(String.format("%d행: 능력치는 %d~%d 사이여야 합니다. (%s)", lineNo, MIN_RATING, MAX_RATING, name));
                continue;
            }
            if (potential < current) {
                warnings.add(String.format("%d행: %s의 최대 능력(%d)이 현재(%d)보다 낮아 현재 값으로 맞췄습니다.",
                        lineNo, name, potential, current));
            }
            Player player = new Player(name, age, position, current, potential, false).withRole(role);
            if (cols.size() > 10 && !cols.get(10).isBlank()) {
                List<Role> extra = new ArrayList<>();
                for (String token : cols.get(10).split("[/,;\\s]+")) {
                    Role r = Role.parse(token);
                    if (r == null) {
                        warnings.add(String.format("%d행: 보조 포지션 '%s'을(를) 알 수 없어 건너뜁니다. (%s)", lineNo, token, name));
                    } else {
                        extra.add(r);
                    }
                }
                player.withSecondary(extra);
            }
            applyProfile(player, team, cols, lineNo, warnings);
            if (cols.size() > 11 && cols.subList(11, cols.size()).stream().anyMatch(c -> !c.isBlank())) {
                int[] attrs = new int[Attributes.NAMES.length];
                boolean ok = cols.size() >= 11 + attrs.length;
                for (int i = 0; ok && i < attrs.length; i++) {
                    Integer v = parseInt(cols.get(11 + i));
                    ok = v != null && v >= 1 && v <= MAX_RATING;
                    attrs[i] = ok ? v : 0;
                }
                if (ok) {
                    player.setAttributes(attrs);
                } else {
                    warnings.add(String.format("%d행: %s의 세부 능력(스피드~피지컬)은 6칸 모두 1~%d 숫자여야 합니다. 자동 값을 씁니다.",
                            lineNo, name, MAX_RATING));
                }
            }
            team.addPlayer(player);
            generator.reserveName(name);
            loaded++;
        }

        int loadedTeams = 0;
        int generatedTeams = 0;
        int generated = 0;
        for (Team team : teams) {
            if (team.squad().isEmpty()) {
                generator.fillSquad(team);
                generatedTeams++;
                continue;
            }
            loadedTeams++;
            int added = generator.topUp(team);
            if (added > 0) {
                warnings.add(String.format("%s: 포지션별 최소 인원(GK 2, DF 6, MF 6, FW 4)이 모자라 %d명을 자동 생성했습니다.",
                        team.name(), added));
                generated += added;
            }
        }
        return new Result(path, true, loaded, loadedTeams, generatedTeams, generated, warnings);
    }

    private static void applyProfile(Player p, Team team, List<String> cols, int lineNo, List<String> warnings) {
        String nationality = cols.size() > 6 && !cols.get(6).isBlank() ? cols.get(6).strip() : "";
        int height = p.height();
        int weight = p.weight();
        Foot foot = p.foot();
        if (cols.size() > 7 && !cols.get(7).isBlank()) {
            Integer h = parseInt(cols.get(7));
            if (h != null && h >= 150 && h <= 215) {
                height = h;
                weight = height - 105;
            } else {
                warnings.add(String.format("%d행: 키 '%s'은(는) 150~215 사이 숫자여야 해서 기본값을 썼습니다.", lineNo, cols.get(7)));
            }
        }
        if (cols.size() > 8 && !cols.get(8).isBlank()) {
            Integer w = parseInt(cols.get(8));
            if (w != null && w >= 45 && w <= 130) {
                weight = w;
            } else {
                warnings.add(String.format("%d행: 몸무게 '%s'은(는) 45~130 사이 숫자여야 해서 기본값을 썼습니다.", lineNo, cols.get(8)));
            }
        }
        if (cols.size() > 9 && !cols.get(9).isBlank()) {
            Foot f = Foot.parse(cols.get(9));
            if (f != null) {
                foot = f;
            } else {
                warnings.add(String.format("%d행: 주발 '%s'을(를) 알 수 없어 오른발로 두었습니다. (L, R, B 또는 왼발·오른발·양발)",
                        lineNo, cols.get(9)));
            }
        }
        p.withProfile(nationality, height, weight, foot);
    }

    private static List<String> readLines(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        int offset = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, bytes.length - offset);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer)
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, offset, bytes.length - offset, Charset.forName("MS949"));
        }
        return text.lines().toList();
    }

    private static List<String> splitCsv(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cols.add(cur.toString().strip());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString().strip());
        return cols;
    }

    private static boolean isHeader(String first) {
        String s = first.strip().toLowerCase();
        return s.equals("구단") || s.equals("팀") || s.equals("team") || s.equals("club");
    }

    private static String normalize(String s) {
        return s.replace(" ", "").strip().toLowerCase();
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Position parsePosition(String s) {
        String p = s.strip().toUpperCase();
        return switch (p) {
            case "GK", "골키퍼" -> Position.GK;
            case "DF", "CB", "LB", "RB", "LWB", "RWB", "수비수", "수비" -> Position.DF;
            case "MF", "CM", "DM", "CDM", "AM", "CAM", "LM", "RM", "미드필더", "미드" -> Position.MF;
            case "FW", "ST", "CF", "LW", "RW", "공격수", "공격" -> Position.FW;
            default -> null;
        };
    }
}
