package superleague.game;

import java.util.List;
import java.util.Random;
import superleague.model.Team;
import superleague.sim.MatchEvent;
import superleague.sim.MatchResult;

final class Reactions {
    private Reactions() {
    }

    static List<String> of(MatchResult r, Team team, String standing, Random random) {
        Team opp = r.fixture().opponentOf(team);
        int gf = r.goalsFor(team);
        int ga = r.goalsAgainst(team);
        int gap = opp.tier().ordinal() - team.tier().ordinal();
        boolean home = r.fixture().home() == team;
        String hero = hero(r, team);

        String fans;
        String media;
        if (gf > ga) {
            if (gap < 0) {
                fans = pick(random, "믿을 수 없다! " + opp.name() + "를 잡았다! 오늘 밤은 잠 못 잔다!",
                        "강팀 " + opp.name() + " 격파! 서포터석이 축제 분위기입니다.");
                media = pick(random, "[이변] " + team.name() + ", 강호 " + opp.name() + " 꺾고 파란",
                        "[분석] 약체 평가 뒤집은 " + team.name() + "의 전술 승리");
            } else if (gf - ga >= 3) {
                fans = pick(random, gf + "골 폭격! 이게 우리 팀이다!", "대승이다! 시즌권 산 보람이 있네.");
                media = pick(random, "[완승] " + team.name() + ", " + opp.name() + " 상대로 " + gf + "-" + ga + " 대승",
                        "[리뷰] 공수 모두 압도한 " + team.name() + ", 완벽한 90분");
            } else {
                fans = pick(random, "깔끔한 승리! 승점 3점 챙겼다.", home ? "홈에서 이기는 맛! 다음 경기도 간다!" : "원정 승리! 원정 팬들 고생했어요!");
                media = pick(random, "[경기 결과] " + team.name() + ", " + opp.name() + " 꺾고 " + standing,
                        "[리뷰] " + team.name() + ", 필요한 순간 결정력 발휘");
            }
        } else if (gf == ga) {
            if (gap < 0) {
                fans = pick(random, opp.name() + " 상대로 비긴 건 충분히 잘한 거다.", "강팀 상대 승점 1점, 나쁘지 않아!");
                media = "[리뷰] " + team.name() + ", 강호 " + opp.name() + "와 대등한 승부";
            } else if (gap > 0) {
                fans = pick(random, "이걸 못 이기냐... 답답하다.", "약팀 상대로 무승부라니, 감독 전술 다시 봐야 한다.");
                media = "[아쉬움] " + team.name() + ", 한 수 아래 " + opp.name() + "와 비겨 승점 2점 놓쳐";
            } else {
                fans = pick(random, "팽팽한 경기였다. 다음엔 이기자.", "무승부... 그래도 지지 않은 게 어디야.");
                media = "[경기 결과] " + team.name() + "-" + opp.name() + ", " + gf + "-" + ga + " 무승부";
            }
        } else {
            if (ga - gf >= 3) {
                fans = pick(random, "참담하다. 오늘 경기는 잊고 싶다.", "이건 아니다. 선수들 정신 차려야 한다!");
                media = pick(random, "[충격] " + team.name() + ", " + opp.name() + "에 " + gf + "-" + ga + " 완패",
                        "[비판] 무너진 수비, " + team.name() + " 감독 전술에 물음표");
            } else if (gap > 0) {
                fans = pick(random, "약팀한테 지다니... 감독 나와라!", "이 경기는 변명의 여지가 없다.");
                media = "[이변] " + team.name() + ", 하위 전력 " + opp.name() + "에 덜미";
            } else {
                fans = pick(random, "아쉽지만 다음 경기가 있다. 힘내자!", "졌지만 잘 싸웠다. 고개 숙이지 마라.");
                media = "[경기 결과] " + team.name() + ", " + opp.name() + "에 " + gf + "-" + ga + " 패배";
            }
        }
        if (hero != null && gf >= ga) {
            fans += " " + hero;
        }
        return List.of("팬 반응: \"" + fans + "\"", "언론: " + media);
    }

    private static String hero(MatchResult r, Team team) {
        java.util.Map<String, Integer> goals = new java.util.HashMap<>();
        for (MatchEvent e : r.events()) {
            if (e.type() == MatchEvent.Type.GOAL && !e.ownGoal() && e.player() != null
                    && team.squad().contains(e.player())) {
                goals.merge(e.player().name(), 1, Integer::sum);
            }
        }
        return goals.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(en -> en.getValue() >= 3 ? en.getKey() + " 해트트릭! 오늘의 영웅!"
                        : en.getValue() == 2 ? en.getKey() + " 멀티골 미쳤다!" : null)
                .orElse(null);
    }

    private static String pick(Random random, String... options) {
        return options[random.nextInt(options.length)];
    }
}
