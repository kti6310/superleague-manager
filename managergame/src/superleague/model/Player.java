package superleague.model;

import java.util.List;

public class Player {
    private final String name;
    private int age;
    private final Position position;
    private final int startRating;
    private final int potential;
    private final boolean generated;
    private String nationality;
    private int height;
    private int weight;
    private Foot foot;
    private Role role;
    private List<Role> secondary = List.of();
    private int[] attributeOffsets;
    private int focus = -1;
    private Role learning;
    private int learningProgress;
    private int trainedGain;
    public static final int ROUNDS_TO_LEARN = 8;
    private int rating;
    private int condition = 100;
    private int injuredRounds = 0;
    private int goals = 0;
    private int appearances = 0;

    public Player(String name, int age, Position position, int rating, int potential, boolean generated) {
        this.name = name;
        this.age = age;
        this.position = position;
        this.rating = rating;
        this.startRating = rating;
        this.potential = Math.max(rating, potential);
        this.generated = generated;
        this.nationality = "";
        this.height = defaultHeight(position);
        this.weight = this.height - 105;
        this.foot = Foot.RIGHT;
        this.role = Role.defaultFor(position);
    }

    public Role role() {
        return role;
    }

    public int[] attributes() {
        if (attributeOffsets == null) {
            attributeOffsets = Attributes.offsets(this);
        }
        int[] out = new int[attributeOffsets.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = Attributes.clamp(rating + attributeOffsets[i]);
        }
        return out;
    }

    public int attribute(int index) {
        return attributes()[index];
    }

    public int focus() {
        return focus;
    }

    public Role learning() {
        return learning;
    }

    public int learningProgress() {
        return learningProgress;
    }

    public int trainedGain() {
        return trainedGain;
    }

    public void setFocus(int attribute) {
        focus = attribute;
        learning = null;
        learningProgress = 0;
    }

    public void setLearning(Role role) {
        focus = -1;
        learning = role;
        learningProgress = 0;
    }

    public String trainingLabel() {
        if (learning != null) {
            return "포지션: " + learning.name();
        }
        return focus < 0 ? "없음" : Attributes.NAMES[focus];
    }

    public String individualTraining(java.util.Random random) {
        if (learning != null) {
            learningProgress += age <= 23 ? 2 : 1;
            if (learningProgress >= ROUNDS_TO_LEARN) {
                Role learned = learning;
                java.util.List<Role> roles = new java.util.ArrayList<>(secondary);
                roles.add(learned);
                withSecondary(roles);
                learning = null;
                learningProgress = 0;
                return String.format("%s 선수가 %s 포지션을 익혔습니다.", name, learned.name());
            }
            return null;
        }
        if (focus < 0) {
            return null;
        }
        double chance = 0.35 * (age <= 23 ? 1.3 : age <= 29 ? 1.0 : 0.6);
        int[] now = attributes();
        if (now[focus] >= 200 || random.nextDouble() >= chance) {
            return null;
        }
        attributeOffsets[focus] += 2;
        trainedGain += 2;
        return String.format("%s %s +2 (%d)", name, Attributes.NAMES[focus], attribute(focus));
    }

    public void setAttributes(int[] values) {
        attributeOffsets = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            attributeOffsets[i] = values[i] - rating;
        }
    }

    public List<Role> secondary() {
        return secondary;
    }

    public String positions() {
        StringBuilder sb = new StringBuilder(role.name());
        for (Role r : secondary) {
            sb.append(", ").append(r.name());
        }
        return sb.toString();
    }

    public Player withSecondary(List<Role> roles) {
        List<Role> out = new java.util.ArrayList<>();
        for (Role r : roles) {
            if (r != null && r != role && !out.contains(r) && (r == Role.GK) == (position == Position.GK)) {
                out.add(r);
            }
        }
        this.secondary = List.copyOf(out);
        return this;
    }

    public Player withRole(Role role) {
        if (role != null && role.group() == position) {
            this.role = role;
        }
        return this;
    }

    public static int defaultHeight(Position position) {
        return switch (position) {
            case GK -> 190;
            case DF -> 185;
            case MF -> 179;
            case FW -> 181;
        };
    }

    public Player withProfile(String nationality, int height, int weight, Foot foot) {
        this.nationality = nationality == null ? "" : nationality;
        this.height = height;
        this.weight = weight;
        this.foot = foot;
        return this;
    }

    public String nationality() {
        return nationality;
    }

    public int height() {
        return height;
    }

    public int weight() {
        return weight;
    }

    public Foot foot() {
        return foot;
    }

    public double bmi() {
        double m = height / 100.0;
        return weight / (m * m);
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    public Position position() {
        return position;
    }

    public int rating() {
        return rating;
    }

    public int potential() {
        return potential;
    }

    public int startRating() {
        return startRating;
    }

    public int growth() {
        return rating - startRating;
    }

    public boolean isGenerated() {
        return generated;
    }

    public int condition() {
        return condition;
    }

    public int injuredRounds() {
        return injuredRounds;
    }

    public int goals() {
        return goals;
    }

    public int appearances() {
        return appearances;
    }

    public boolean isAvailable() {
        return injuredRounds == 0;
    }

    public double effectiveRating() {
        return rating * (0.85 + 0.15 * condition / 100.0);
    }

    public double growthChance(int teamTraining) {
        if (rating >= potential || !isAvailable()) {
            return 0;
        }
        double ageFactor;
        if (age <= 21) {
            ageFactor = 1.5;
        } else if (age <= 24) {
            ageFactor = 1.0;
        } else if (age <= 28) {
            ageFactor = 0.5;
        } else if (age <= 31) {
            ageFactor = 0.2;
        } else {
            ageFactor = 0.0;
        }
        double gapFactor = Math.min(1.0, (potential - rating) / 20.0);
        double trainingFactor = 0.4 + teamTraining / 100.0;
        return 0.05 * ageFactor * gapFactor * trainingFactor;
    }

    public void grow() {
        rating = Math.min(potential, rating + 2);
    }

    public int newSeason(java.util.Random random) {
        age++;
        goals = 0;
        appearances = 0;
        condition = 100;
        injuredRounds = 0;
        if (age < 31) {
            return 0;
        }
        int drop = Math.min(rating - 30, random.nextInt(2 * (age - 29)));
        rating -= drop;
        return drop;
    }

    public void changeCondition(int delta) {
        condition = Math.max(40, Math.min(100, condition + delta));
    }

    public void injure(int rounds) {
        injuredRounds = Math.max(injuredRounds, rounds);
    }

    public void passRound() {
        if (injuredRounds > 0) {
            injuredRounds--;
        }
    }

    public void addGoal() {
        goals++;
    }

    public void addAppearance() {
        appearances++;
    }
}
