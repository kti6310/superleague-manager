package superleague.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Team {
    public static final int MAX_FACILITY = 5;

    private final String name;
    private final String country;
    private final Tier tier;
    private final List<Player> squad = new ArrayList<>();

    private int morale = 0;
    private int training = 50;
    private int facility = 1;
    private Tactic tactic = Tactic.BALANCED;
    private Tactic preferredTactic = Tactic.BALANCED;
    private TrainingIntensity intensity = TrainingIntensity.NORMAL;
    private TeamTraining teamTraining = TeamTraining.BALANCED;
    private Formation formation = Formation.F4_4_2;
    private List<Formation.Slot> customSlots;
    private final TacticalRole[] duties = new TacticalRole[11];
    private Player[] lineup = new Player[11];
    private final Instructions instructions = new Instructions();
    private boolean autoLineup = true;
    private String managerName;
    private ManagerStyle managerStyle;

    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;

    public Team(String name, String country, Tier tier) {
        this.name = name;
        this.country = country;
        this.tier = tier;
    }

    public String name() {
        return name;
    }

    public String country() {
        return country;
    }

    public Tier tier() {
        return tier;
    }

    public List<Player> squad() {
        return squad;
    }

    public void addPlayer(Player player) {
        squad.add(player);
    }

    public int morale() {
        return morale;
    }

    public int training() {
        return training;
    }

    public int facility() {
        return facility;
    }

    public Tactic tactic() {
        return tactic;
    }

    public void setTactic(Tactic tactic) {
        this.tactic = tactic;
    }

    public Tactic preferredTactic() {
        return preferredTactic;
    }

    public void setPreferredTactic(Tactic preferredTactic) {
        this.preferredTactic = preferredTactic;
    }

    public TeamTraining teamTraining() {
        return teamTraining;
    }

    public void setTeamTraining(TeamTraining teamTraining) {
        this.teamTraining = teamTraining;
    }

    public TrainingIntensity intensity() {
        return intensity;
    }

    public void setIntensity(TrainingIntensity intensity) {
        this.intensity = intensity;
    }

    public void changeMorale(int delta) {
        morale = clamp(morale + delta, -100, 100);
    }

    public void decayMorale() {
        morale = (int) Math.round(morale * 0.92);
    }

    public void changeTraining(int delta) {
        training = clamp(training + delta, 0, trainingCap());
    }

    public int trainingCap() {
        return Math.min(100, 60 + facility * 8);
    }

    public boolean upgradeFacility() {
        if (facility >= MAX_FACILITY) {
            return false;
        }
        facility++;
        return true;
    }

    public double moraleModifier() {
        return 0.08 * morale / 100.0;
    }

    public double trainingModifier() {
        return -0.05 + 0.12 * training / 100.0;
    }

    public double facilityModifier() {
        return 0.01 * facility;
    }

    public Formation formation() {
        return formation;
    }

    public TacticalRole duty(int index) {
        String label = slots().get(index).label();
        TacticalRole r = duties[index];
        return r != null && r.fits(label) ? r : TacticalRole.defaultFor(label);
    }

    public void setDuty(int index, TacticalRole role) {
        if (role == null || role.fits(slots().get(index).label())) {
            duties[index] = role;
        }
    }

    public TacticalRole dutyOf(Player p) {
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] == p) {
                return duty(i);
            }
        }
        return null;
    }

    public double dutyAttackFactor() {
        double sum = 0;
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] != null) {
                sum += duty(i).attack;
            }
        }
        return 1 + sum / 100.0;
    }

    public double dutyConcedeFactor() {
        double sum = 0;
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] != null) {
                sum += duty(i).defense;
            }
        }
        return 1 - sum / 100.0;
    }

    public List<Formation.Slot> slots() {
        return formation == Formation.CUSTOM ? customSlots : formation.slots();
    }

    public List<Formation.Slot> customSlots() {
        return customSlots;
    }

    public void moveSlot(int index, Formation.Slot target) {
        if (formation != Formation.CUSTOM) {
            customSlots = new ArrayList<>(slots());
            formation = Formation.CUSTOM;
        }
        customSlots.set(index, target);
    }

    public void setFormation(Formation formation) {
        if (formation == this.formation) {
            return;
        }
        if (formation == Formation.CUSTOM && customSlots == null) {
            customSlots = new ArrayList<>(slots());
        }
        List<Player> keep = startingEleven();
        this.formation = formation;
        this.lineup = new Player[11];
        autoPick(keep);
    }

    public boolean isAutoLineup() {
        return autoLineup;
    }

    public void setAutoLineup(boolean autoLineup) {
        this.autoLineup = autoLineup;
    }

    public Instructions instructions() {
        return instructions;
    }

    public Player slotPlayer(int index) {
        return lineup[index];
    }

    public void setSlot(int index, Player player) {
        for (int i = 0; i < lineup.length; i++) {
            if (i != index && lineup[i] == player) {
                lineup[i] = lineup[index];
            }
        }
        lineup[index] = player;
    }

    public void autoPick() {
        lineup = new Player[11];
        autoPick(List.of());
    }

    private void autoPick(List<Player> preferred) {
        List<Formation.Slot> slots = slots();
        for (Player p : preferred) {
            if (!p.isAvailable() || contains(p)) {
                continue;
            }
            int best = -1;
            double bestFit = 0.97;
            for (int i = 0; i < slots.size(); i++) {
                if (lineup[i] == null && Formation.eligible(p, slots.get(i)) && Formation.fit(p, slots.get(i)) > bestFit) {
                    bestFit = Formation.fit(p, slots.get(i));
                    best = i;
                }
            }
            if (best >= 0) {
                lineup[best] = p;
            }
        }
        for (boolean sameGroupOnly : new boolean[]{true, false}) {
            fill(slots, sameGroupOnly);
        }
    }

    private void fill(List<Formation.Slot> slots, boolean sameGroupOnly) {
        while (true) {
            int bestSlot = -1;
            Player bestPlayer = null;
            double bestScore = -1;
            for (int i = 0; i < slots.size(); i++) {
                if (lineup[i] != null) {
                    continue;
                }
                for (Player p : squad) {
                    if (!p.isAvailable() || contains(p)) {
                        continue;
                    }
                    if (sameGroupOnly && !Formation.eligible(p, slots.get(i))) {
                        continue;
                    }
                    if ((p.position() == Position.GK) != (slots.get(i).group() == Position.GK)) {
                        continue;
                    }
                    double score = p.effectiveRating() * Formation.fit(p, slots.get(i));
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = i;
                        bestPlayer = p;
                    }
                }
            }
            if (bestSlot < 0) {
                return;
            }
            lineup[bestSlot] = bestPlayer;
        }
    }

    private boolean contains(Player p) {
        for (Player q : lineup) {
            if (q == p) {
                return true;
            }
        }
        return false;
    }

    public List<String> validateLineup() {
        List<String> changes = new ArrayList<>();
        List<Formation.Slot> slots = slots();
        for (int i = 0; i < lineup.length; i++) {
            Player p = lineup[i];
            if (p != null && p.isAvailable() && squad.contains(p)) {
                continue;
            }
            Player best = null;
            double bestScore = -1;
            for (Player q : squad) {
                if (!q.isAvailable() || contains(q)) {
                    continue;
                }
                double score = q.effectiveRating() * Formation.fit(q, slots.get(i))
                        * (Formation.eligible(q, slots.get(i)) ? 1.0 : 0.5);
                if ((q.position() == Position.GK) != (slots.get(i).group() == Position.GK)) {
                    continue;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = q;
                }
            }
            lineup[i] = best;
            if (p != null && best != null) {
                changes.add(String.format("%s(%s) 결장으로 %s 선수가 대신 출전합니다.", p.name(), slots.get(i).label(), best.name()));
            }
        }
        return changes;
    }

    public List<Player> startingEleven() {
        List<Player> xi = new ArrayList<>();
        for (Player p : lineup) {
            if (p != null) {
                xi.add(p);
            }
        }
        return xi;
    }

    public double slotScore(int index) {
        Player p = lineup[index];
        if (p == null) {
            return 50;
        }
        return p.effectiveRating() * Formation.fit(p, slots().get(index));
    }

    public double lineupStrength() {
        double sum = 0;
        for (int i = 0; i < lineup.length; i++) {
            sum += slotScore(i);
        }
        return sum / lineup.length;
    }

    public int nationalityPairs() {
        Map<String, Integer> counts = new HashMap<>();
        for (Player p : startingEleven()) {
            if (!p.nationality().isBlank()) {
                counts.merge(p.nationality(), 1, Integer::sum);
            }
        }
        int pairs = 0;
        for (int c : counts.values()) {
            pairs += c * (c - 1) / 2;
        }
        return pairs;
    }

    public double chemistryModifier() {
        return Math.min(0.03, 0.0015 * nationalityPairs()) + (teamworkRounds > 0 ? TEAMWORK_BONUS : 0)
                + (managerStyle == ManagerStyle.SYSTEM_BUILDER ? 0.015 : 0);
    }

    public static final double TEAMWORK_BONUS = 0.03;

    public void setManager(String name, ManagerStyle style) {
        managerName = name;
        managerStyle = style;
    }

    public String managerName() {
        return managerName;
    }

    public ManagerStyle managerStyle() {
        return managerStyle;
    }

    public double managerAttackFactor(Team opponent) {
        if (managerStyle == ManagerStyle.TACTICIAN && opponent.tier().isHigherThan(tier)) {
            return 1.07;
        }
        return managerStyle == ManagerStyle.PRAGMATIST ? 0.98 : 1;
    }

    public double managerConcedeFactor(Team opponent) {
        if (managerStyle == ManagerStyle.TACTICIAN && opponent.tier().isHigherThan(tier)) {
            return 0.93;
        }
        return managerStyle == ManagerStyle.PRAGMATIST ? 0.94 : 1;
    }

    public double counterBonusExtra() {
        return managerStyle == ManagerStyle.STRATEGIST ? 0.15 : 0;
    }
    private int teamworkRounds;
    private int trainingBoostRounds;

    public int teamworkRounds() {
        return teamworkRounds;
    }

    public int trainingBoostRounds() {
        return trainingBoostRounds;
    }

    public void grantTeamwork(int rounds) {
        teamworkRounds = Math.max(teamworkRounds, rounds);
    }

    public void grantTrainingBoost(int rounds) {
        trainingBoostRounds = Math.max(trainingBoostRounds, rounds);
    }

    public void passPerkRound() {
        teamworkRounds = Math.max(0, teamworkRounds - 1);
        trainingBoostRounds = Math.max(0, trainingBoostRounds - 1);
    }

    public void resetSeason() {
        played = won = drawn = lost = goalsFor = goalsAgainst = 0;
        teamworkRounds = trainingBoostRounds = 0;
        morale = morale / 2;
    }

    public double aerialHeight() {
        return startingEleven().stream()
                .filter(p -> p.position() != Position.GK)
                .mapToInt(Player::height)
                .average()
                .orElse(180);
    }

    public long footMismatches() {
        long n = 0;
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] != null && Formation.footFit(lineup[i].foot(), slots().get(i)) < 1.0) {
                n++;
            }
        }
        return n;
    }

    public long roleMismatches() {
        long n = 0;
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] != null && Formation.roleValue(lineup[i], slots().get(i).label()) < Formation.SECONDARY_FIT) {
                n++;
            }
        }
        return n;
    }

    public double averageFit() {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < lineup.length; i++) {
            if (lineup[i] != null) {
                sum += Formation.fit(lineup[i], slots().get(i));
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    public double performance() {
        return lineupStrength() * (1 + moraleModifier() + trainingModifier() + facilityModifier() + chemistryModifier());
    }

    public double averageCondition() {
        return squad.stream().mapToInt(Player::condition).average().orElse(100);
    }

    public long injuredCount() {
        return squad.stream().filter(p -> !p.isAvailable()).count();
    }

    public Player starPlayer() {
        return squad.stream().max(Comparator.comparingInt(Player::rating)).orElseThrow();
    }

    public void recordResult(int scored, int conceded) {
        played++;
        goalsFor += scored;
        goalsAgainst += conceded;
        if (scored > conceded) {
            won++;
        } else if (scored == conceded) {
            drawn++;
        } else {
            lost++;
        }
    }

    public int played() {
        return played;
    }

    public int won() {
        return won;
    }

    public int drawn() {
        return drawn;
    }

    public int lost() {
        return lost;
    }

    public int goalsFor() {
        return goalsFor;
    }

    public int goalsAgainst() {
        return goalsAgainst;
    }

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }

    public int points() {
        return won * 3 + drawn;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
