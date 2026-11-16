package superleague.model;

public enum TrainingIntensity {
    LIGHT("가벼운 훈련", -2, 4, 12, 0.004),
    NORMAL("일반 훈련", 2, 0, 6, 0.010),
    INTENSE("강도 높은 훈련", 5, -5, -2, 0.030);

    private final String label;
    private final int trainingDelta;
    private final int moraleDelta;
    private final int conditionDelta;
    private final double injuryChance;

    TrainingIntensity(String label, int trainingDelta, int moraleDelta, int conditionDelta, double injuryChance) {
        this.label = label;
        this.trainingDelta = trainingDelta;
        this.moraleDelta = moraleDelta;
        this.conditionDelta = conditionDelta;
        this.injuryChance = injuryChance;
    }

    public String label() {
        return label;
    }

    public int trainingDelta() {
        return trainingDelta;
    }

    public int moraleDelta() {
        return moraleDelta;
    }

    public int conditionDelta() {
        return conditionDelta;
    }

    public double injuryChance() {
        return injuryChance;
    }

    public String summary() {
        return String.format("%s (훈련 %+d, 사기 %+d, 컨디션 %+d, 부상 위험 %.1f%%)",
                label, trainingDelta, moraleDelta, conditionDelta, injuryChance * 100);
    }
}
