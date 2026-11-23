package superleague.game;

import java.util.List;
import superleague.model.Player;

public record OwnerReview(String grade, String headline, List<String> lines, double points, double expected,
                          boolean reward, Player mvp, String penalty) {

    static String grade(double diff) {
        return diff >= 2 ? "대만족" : diff >= 0 ? "만족" : diff >= -2 ? "보통" : "실망";
    }

    static double expectedPoints(int tierGap) {
        return Math.max(0.6, Math.min(2.2, 1.4 + 0.45 * tierGap));
    }
}
