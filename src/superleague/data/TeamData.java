package superleague.data;

import java.util.ArrayList;
import java.util.List;
import superleague.model.Team;
import superleague.model.Tier;

public final class TeamData {
    private TeamData() {
    }

    public static List<Team> createTeams() {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team("로스 블랑코스", "스페인", Tier.S));
        teams.add(new Team("맨체스터 블루스", "잉글랜드", Tier.S));
        teams.add(new Team("디 바이언", "독일", Tier.S));
        teams.add(new Team("더 레즈", "잉글랜드", Tier.S));
        teams.add(new Team("르 파리지앵", "프랑스", Tier.S));
        teams.add(new Team("런던 거너스", "잉글랜드", Tier.A));
        teams.add(new Team("바르사", "스페인", Tier.A));
        teams.add(new Team("밀라노 네라주리", "이탈리아", Tier.A));
        teams.add(new Team("런던 블루스", "잉글랜드", Tier.A));
        teams.add(new Team("알레띠", "스페인", Tier.A));
        teams.add(new Team("맨체스터 레드", "잉글랜드", Tier.B));
        teams.add(new Team("비안코네리", "이탈리아", Tier.B));
        teams.add(new Team("밀라노 로소네리", "이탈리아", Tier.B));
        teams.add(new Team("런던 스퍼스", "잉글랜드", Tier.B));
        teams.add(new Team("디 보루센", "독일", Tier.B));
        teams.add(new Team("라젠발", "독일", Tier.C));
        teams.add(new Team("아슬란", "튀르키예", Tier.C));
        teams.add(new Team("코모", "이탈리아", Tier.C));
        return teams;
    }
}
