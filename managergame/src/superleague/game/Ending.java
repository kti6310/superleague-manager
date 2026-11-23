package superleague.game;

import superleague.model.Player;
import superleague.model.Team;

public record Ending(String title, String description, boolean starStays, Player star) {

    public boolean fired() {
        return title.equals("경질");
    }

    public static Ending career(Team team, int titles, int targetsMet, int seasons) {
        Player star = team.starPlayer();
        boolean stays = team.morale() >= 0;
        if (titles >= 3) {
            return new Ending("왕조의 설계자", String.format("%d시즌 동안 우승 %d회. 구단은 경기장 앞에 감독의 동상을 세우기로 했습니다.", seasons, titles), stays, star);
        }
        if (titles >= 1) {
            return new Ending("우승 감독", String.format("%d시즌 동안 우승 %d회, 목표 달성 %d회. 구단의 전설적인 감독으로 남았습니다.", seasons, titles, targetsMet), stays, star);
        }
        if (targetsMet >= seasons - 1) {
            return new Ending("믿음직한 장기 집권", String.format("%d시즌 중 %d시즌 목표 달성. 구단주가 종신 계약을 제안했습니다.", seasons, targetsMet), stays, star);
        }
        if (targetsMet >= seasons / 2 + 1) {
            return new Ending("무난한 5년", String.format("%d시즌 중 %d시즌 목표 달성. 다음 감독에게 안정된 팀을 넘겨줬습니다.", seasons, targetsMet), stays, star);
        }
        return new Ending("아쉬운 작별", String.format("%d시즌 중 목표 달성은 %d회. 계약 만료와 함께 팀을 떠납니다.", seasons, targetsMet), stays, star);
    }

    public static Ending decide(Team team, int rank) {
        int diff = team.tier().targetRank() - rank;
        int morale = team.morale();
        Player star = team.starPlayer();
        boolean starStays = morale >= 0;

        if (rank == 1) {
            return new Ending("리그 챔피언",
                    "슈퍼리그 우승. 구단 역사에 이름을 남기고 장기 재계약을 제안받았습니다.", starStays, star);
        }
        if (diff >= 3 && morale >= 20) {
            return new Ending("기적의 시즌",
                    "등급을 뛰어넘는 성적과 단단한 선수단. 운영진이 장기 재계약과 특별 보너스를 약속했습니다.", starStays, star);
        }
        if (diff >= 0 && morale < -30) {
            return new Ending("이긴 시즌, 잃은 라커룸",
                    "목표는 달성했지만 선수단의 신뢰를 잃었습니다. 운영진은 1년 연장만 제안했습니다.", starStays, star);
        }
        if (diff >= 0) {
            return new Ending("재계약",
                    "운영진의 목표를 달성했습니다. 다음 시즌도 지휘봉을 잡습니다.", starStays, star);
        }
        if (diff >= -2) {
            return new Ending("유임, 그러나 경고",
                    "목표에 조금 못 미쳤습니다. 다음 시즌 초반 성적이 거취를 결정합니다.", starStays, star);
        }
        return new Ending("경질",
                "운영진의 기대에 크게 못 미쳤습니다. 시즌 종료와 함께 경질됐습니다.", starStays, star);
    }
}
