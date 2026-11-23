package superleague.game;

import java.util.List;
import java.util.Random;
import superleague.model.Player;
import superleague.model.Team;

public class RandomEvents {
    private static final double EVENT_CHANCE = 0.15;

    private final Random random;

    public RandomEvents(Random random) {
        this.random = random;
    }

    public String maybeTrigger(Team team) {
        if (random.nextDouble() >= EVENT_CHANCE) {
            return null;
        }
        switch (random.nextInt(6)) {
            case 0 -> {
                team.changeMorale(6);
                return "주장이 선수단 미팅을 주도했습니다. (사기 +6)";
            }
            case 1 -> {
                team.changeMorale(-6);
                return "SNS에서 선수단 불화설이 돌았습니다. (사기 -6)";
            }
            case 2 -> {
                team.changeTraining(4);
                return "코칭스태프가 새 훈련 프로그램을 도입했습니다. (훈련 수준 +4)";
            }
            case 3 -> {
                team.changeTraining(-4);
                return "훈련장 잔디 보수로 훈련 일정이 꼬였습니다. (훈련 수준 -4)";
            }
            case 4 -> {
                List<Player> xi = team.startingEleven();
                if (xi.isEmpty()) {
                    return null;
                }
                Player p = xi.get(random.nextInt(xi.size()));
                int rounds = 1 + random.nextInt(2);
                p.injure(rounds);
                return String.format("%s 선수가 가벼운 부상을 입었습니다. (%d라운드 결장)", p.name(), rounds);
            }
            default -> {
                team.squad().forEach(p -> p.changeCondition(8));
                return "구단이 선수단 회복 캠프를 지원했습니다. (전원 체력 +8)";
            }
        }
    }
}
