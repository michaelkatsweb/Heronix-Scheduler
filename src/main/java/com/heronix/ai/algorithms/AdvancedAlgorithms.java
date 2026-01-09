package com.heronix.ai.algorithms;

import com.heronix.model.domain.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Advanced Scheduling Algorithms Collection
 * Location:
 * src/main/java/com/eduscheduler/ai/algorithms/AdvancedAlgorithms.java
 * 
 * Contains innovative algorithm implementations for the hybrid solver
 * 
 * @author Heronix Scheduling System Team
 * @version 4.0.0
 */

// ========================================================================
// REINFORCEMENT LEARNING AGENT
// ========================================================================

@Slf4j
class QLearningAgent {
    private final double learningRate;
    private final double discountFactor;
    private final double explorationRate;
    private Map<StateActionPair, Double> qTable;
    private Random random;

    public QLearningAgent(double learningRate, double discountFactor, double explorationRate) {
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.explorationRate = explorationRate;
        this.qTable = new HashMap<>();
        this.random = new Random();
    }

    public ScheduleAction chooseAction(ScheduleState state) {
        // Epsilon-greedy strategy
        if (random.nextDouble() < explorationRate) {
            // Explore: random action
            return ScheduleAction.randomAction();
        } else {
            // Exploit: best known action
            return getBestAction(state);
        }
    }

    public void updateQValue(ScheduleState state, ScheduleAction action,
            double reward, ScheduleState nextState) {
        StateActionPair pair = new StateActionPair(state, action);

        // Current Q-value
        double currentQ = qTable.getOrDefault(pair, 0.0);

        // Maximum Q-value for next state
        double maxNextQ = getMaxQValue(nextState);

        // Q-learning update rule
        double newQ = currentQ + learningRate *
                (reward + discountFactor * maxNextQ - currentQ);

        qTable.put(pair, newQ);
    }

    private ScheduleAction getBestAction(ScheduleState state) {
        ScheduleAction bestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (ScheduleAction action : ScheduleAction.getAllActions()) {
            StateActionPair pair = new StateActionPair(state, action);
            double value = qTable.getOrDefault(pair, 0.0);

            if (value > bestValue) {
                bestValue = value;
                bestAction = action;
            }
        }

        return bestAction != null ? bestAction : ScheduleAction.randomAction();
    }

    private double getMaxQValue(ScheduleState state) {
        double maxQ = Double.NEGATIVE_INFINITY;

        for (ScheduleAction action : ScheduleAction.getAllActions()) {
            StateActionPair pair = new StateActionPair(state, action);
            double q = qTable.getOrDefault(pair, 0.0);
            maxQ = Math.max(maxQ, q);
        }

        return maxQ == Double.NEGATIVE_INFINITY ? 0.0 : maxQ;
    }
}

class ScheduleState {
    private Schedule schedule;
    private Map<String, Object> features;

    public ScheduleState(Schedule schedule) {
        this.schedule = schedule;
        this.features = extractFeatures(schedule);
    }

    private Map<String, Object> extractFeatures(Schedule schedule) {
        Map<String, Object> features = new HashMap<>();

        // Extract relevant state features
        features.put("conflictCount", countConflicts(schedule));
        features.put("utilization", calculateUtilization(schedule));
        features.put("balance", calculateBalance(schedule));
        features.put("timeSlotDistribution", getTimeSlotDistribution(schedule));

        return features;
    }

    private int countConflicts(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null) {
            return 0;
        }

        int conflicts = 0;
        List<ScheduleSlot> slots = schedule.getSlots();

        // Check for teacher conflicts (same teacher, same time)
        Map<String, List<ScheduleSlot>> teacherTimeMap = new HashMap<>();
        for (ScheduleSlot slot : slots) {
            if (slot.getTeacher() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getTeacher().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                teacherTimeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }
        conflicts += teacherTimeMap.values().stream().filter(list -> list.size() > 1).count();

        // Check for room conflicts (same room, same time)
        Map<String, List<ScheduleSlot>> roomTimeMap = new HashMap<>();
        for (ScheduleSlot slot : slots) {
            if (slot.getRoom() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getRoom().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                roomTimeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }
        conflicts += roomTimeMap.values().stream().filter(list -> list.size() > 1).count();

        return conflicts;
    }

    private double calculateUtilization(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null || schedule.getSlots().isEmpty()) {
            return 0.0;
        }

        List<ScheduleSlot> slots = schedule.getSlots();
        long assignedSlots = slots.stream()
            .filter(s -> s.getTeacher() != null && s.getRoom() != null && s.getCourse() != null)
            .count();

        return (double) assignedSlots / slots.size();
    }

    private double calculateBalance(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null || schedule.getSlots().isEmpty()) {
            return 0.0;
        }

        // Calculate workload balance based on teacher slot distribution
        Map<Long, Long> teacherSlotCounts = schedule.getSlots().stream()
            .filter(s -> s.getTeacher() != null)
            .collect(Collectors.groupingBy(
                s -> s.getTeacher().getId(),
                Collectors.counting()
            ));

        if (teacherSlotCounts.isEmpty()) {
            return 0.0;
        }

        // Calculate coefficient of variation (lower = more balanced)
        double mean = teacherSlotCounts.values().stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 0.0;

        double variance = teacherSlotCounts.values().stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0);

        double cv = Math.sqrt(variance) / mean;

        // Convert to 0-1 scale where 1 is perfectly balanced
        return Math.max(0, 1 - cv);
    }

    private Map<String, Integer> getTimeSlotDistribution(Schedule schedule) {
        Map<String, Integer> distribution = new HashMap<>();

        if (schedule == null || schedule.getSlots() == null) {
            return distribution;
        }

        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getDayOfWeek().toString() + "_" + slot.getStartTime().getHour();
                distribution.merge(key, 1, Integer::sum);
            }
        }

        return distribution;
    }

    @Override
    public int hashCode() {
        return features.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ScheduleState))
            return false;
        ScheduleState other = (ScheduleState) obj;
        return features.equals(other.features);
    }
}

class ScheduleAction {
    public enum ActionType {
        SWAP_TEACHERS,
        SWAP_ROOMS,
        SWAP_TIMESLOTS,
        MOVE_CLASS,
        SPLIT_CLASS,
        MERGE_CLASSES,
        REASSIGN_STUDENTS
    }

    private ActionType type;
    private Map<String, Object> parameters;

    public ScheduleAction(ActionType type) {
        this.type = type;
        this.parameters = new HashMap<>();
    }

    public static ScheduleAction randomAction() {
        ActionType[] types = ActionType.values();
        ActionType randomType = types[new Random().nextInt(types.length)];
        return new ScheduleAction(randomType);
    }

    public static List<ScheduleAction> getAllActions() {
        return Arrays.stream(ActionType.values())
                .map(ScheduleAction::new)
                .collect(Collectors.toList());
    }

    // Getters/setters
    public ActionType getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}

class StateActionPair {
    private ScheduleState state;
    private ScheduleAction action;

    public StateActionPair(ScheduleState state, ScheduleAction action) {
        this.state = state;
        this.action = action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, action.getType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof StateActionPair))
            return false;
        StateActionPair other = (StateActionPair) obj;
        return state.equals(other.state) &&
                action.getType().equals(other.action.getType());
    }
}

// ========================================================================
// CONSTRAINT PROPAGATION NETWORK (AC-3 ALGORITHM)
// ========================================================================

class ConstraintNetwork {
    private List<Constraint> constraints;
    private Map<Variable, Domain> domains;
    private Queue<Arc> arcQueue;

    public ConstraintNetwork() {
        this.constraints = new ArrayList<>();
        this.domains = new HashMap<>();
        this.arcQueue = new LinkedList<>();
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public boolean propagate(Schedule schedule) {
        // Initialize domains
        initializeDomains(schedule);

        // Initialize arc queue
        initializeArcQueue();

        // AC-3 algorithm
        while (!arcQueue.isEmpty()) {
            Arc arc = arcQueue.poll();

            if (revise(arc)) {
                if (domains.get(arc.getVariable()).isEmpty()) {
                    // No solution exists
                    return false;
                }

                // Add neighboring arcs to queue
                for (Arc neighbor : getNeighboringArcs(arc)) {
                    if (!arcQueue.contains(neighbor)) {
                        arcQueue.add(neighbor);
                    }
                }
            }
        }

        return true;
    }

    private boolean revise(Arc arc) {
        boolean revised = false;
        Variable var1 = arc.getVariable();
        Variable var2 = arc.getConnectedVariable();
        Domain domain1 = domains.get(var1);
        Domain domain2 = domains.get(var2);

        Iterator<Object> iterator = domain1.getValues().iterator();
        while (iterator.hasNext()) {
            Object value1 = iterator.next();
            boolean hasSupport = false;

            for (Object value2 : domain2.getValues()) {
                if (arc.getConstraint().isSatisfied(var1, value1, var2, value2)) {
                    hasSupport = true;
                    break;
                }
            }

            if (!hasSupport) {
                iterator.remove();
                revised = true;
            }
        }

        return revised;
    }

    private void initializeDomains(Schedule schedule) {
        for (ScheduleSlot slot : schedule.getSlots()) {
            Variable var = new Variable(slot);
            Domain domain = new Domain();

            // Add possible values for each variable
            domain.addPossibleTeachers(getPossibleTeachers(slot));
            domain.addPossibleRooms(getPossibleRooms(slot));
            domain.addPossibleTimeSlots(getPossibleTimeSlots(slot));

            domains.put(var, domain);
        }
    }

    private void initializeArcQueue() {
        for (Constraint constraint : constraints) {
            for (Variable var1 : constraint.getVariables()) {
                for (Variable var2 : constraint.getVariables()) {
                    if (!var1.equals(var2)) {
                        arcQueue.add(new Arc(var1, var2, constraint));
                    }
                }
            }
        }
    }

    private List<Arc> getNeighboringArcs(Arc arc) {
        List<Arc> neighbors = new ArrayList<>();
        Variable var = arc.getVariable();

        for (Constraint constraint : constraints) {
            if (constraint.involves(var)) {
                for (Variable otherVar : constraint.getVariables()) {
                    if (!otherVar.equals(var) &&
                            !otherVar.equals(arc.getConnectedVariable())) {
                        neighbors.add(new Arc(otherVar, var, constraint));
                    }
                }
            }
        }

        return neighbors;
    }

    // Helper methods
    private List<Teacher> getPossibleTeachers(ScheduleSlot slot) {
        // Return teachers qualified for the course
        return new ArrayList<>(); // Simplified
    }

    private List<Room> getPossibleRooms(ScheduleSlot slot) {
        // Return rooms suitable for the class
        return new ArrayList<>(); // Simplified
    }

    private List<TimeSlot> getPossibleTimeSlots(ScheduleSlot slot) {
        // Return available time slots
        return new ArrayList<>(); // Simplified
    }
}

abstract class Constraint {
    protected List<Variable> variables;

    public Constraint() {
        this.variables = new ArrayList<>();
    }

    public abstract boolean isSatisfied(Variable var1, Object val1,
            Variable var2, Object val2);

    public boolean involves(Variable var) {
        return variables.contains(var);
    }

    public List<Variable> getVariables() {
        return variables;
    }
}

class TeacherAvailabilityConstraint extends Constraint {
    @Override
    public boolean isSatisfied(Variable var1, Object val1,
            Variable var2, Object val2) {
        // Check if teacher is available at the time slot
        return true; // Simplified
    }
}

class RoomCapacityConstraint extends Constraint {
    @Override
    public boolean isSatisfied(Variable var1, Object val1,
            Variable var2, Object val2) {
        // Check if room capacity is sufficient
        return true; // Simplified
    }
}

class Variable {
    private ScheduleSlot slot;

    public Variable(ScheduleSlot slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Variable))
            return false;
        Variable other = (Variable) obj;
        return slot.equals(other.slot);
    }

    @Override
    public int hashCode() {
        return slot.hashCode();
    }
}

class Domain {
    private Set<Object> values;

    public Domain() {
        this.values = new HashSet<>();
    }

    public void addPossibleTeachers(List<Teacher> teachers) {
        values.addAll(teachers);
    }

    public void addPossibleRooms(List<Room> rooms) {
        values.addAll(rooms);
    }

    public void addPossibleTimeSlots(List<TimeSlot> timeSlots) {
        values.addAll(timeSlots);
    }

    public Set<Object> getValues() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}

class Arc {
    private Variable variable;
    private Variable connectedVariable;
    private Constraint constraint;

    public Arc(Variable variable, Variable connectedVariable, Constraint constraint) {
        this.variable = variable;
        this.connectedVariable = connectedVariable;
        this.constraint = constraint;
    }

    // Getters
    public Variable getVariable() {
        return variable;
    }

    public Variable getConnectedVariable() {
        return connectedVariable;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Arc))
            return false;
        Arc other = (Arc) obj;
        return variable.equals(other.variable) &&
                connectedVariable.equals(other.connectedVariable);
    }
}

// ========================================================================
// WELSH-POWELL GRAPH COLORING
// ========================================================================

@Slf4j
class GraphColoringScheduler {

    public Map<ScheduleSlot, Integer> welshPowellColoring(ConflictGraph graph) {
        log.info("Applying Welsh-Powell algorithm for graph coloring");

        Map<ScheduleSlot, Integer> coloring = new HashMap<>();
        Set<ScheduleSlot> vertices = graph.getVertices();

        // Sort vertices by degree (descending)
        List<ScheduleSlot> sortedVertices = vertices.stream()
                .sorted((v1, v2) -> Integer.compare(
                        graph.getDegree(v2),
                        graph.getDegree(v1)))
                .collect(Collectors.toList());

        // Assign colors
        int currentColor = 0;
        Set<ScheduleSlot> uncolored = new HashSet<>(sortedVertices);

        while (!uncolored.isEmpty()) {
            Set<ScheduleSlot> coloredThisRound = new HashSet<>();

            for (ScheduleSlot vertex : sortedVertices) {
                if (!uncolored.contains(vertex))
                    continue;

                // Check if vertex can be colored with current color
                boolean canColor = true;
                for (ScheduleSlot neighbor : graph.getNeighbors(vertex)) {
                    if (coloring.getOrDefault(neighbor, -1) == currentColor) {
                        canColor = false;
                        break;
                    }
                }

                if (canColor) {
                    coloring.put(vertex, currentColor);
                    coloredThisRound.add(vertex);
                }
            }

            uncolored.removeAll(coloredThisRound);
            currentColor++;
        }

        log.info("Graph colored with {} colors", currentColor);
        return coloring;
    }

    public Map<ScheduleSlot, Integer> dsaturColoring(ConflictGraph graph) {
        log.info("Applying DSATUR algorithm for graph coloring");

        Map<ScheduleSlot, Integer> coloring = new HashMap<>();
        Map<ScheduleSlot, Set<Integer>> saturation = new HashMap<>();

        // Initialize saturation degrees
        for (ScheduleSlot vertex : graph.getVertices()) {
            saturation.put(vertex, new HashSet<>());
        }

        while (coloring.size() < graph.getVertices().size()) {
            // Select vertex with highest saturation degree
            ScheduleSlot nextVertex = selectNextVertex(graph, coloring, saturation);

            // Find smallest available color
            int color = findSmallestColor(nextVertex, graph, coloring);

            // Assign color
            coloring.put(nextVertex, color);

            // Update saturation degrees of neighbors
            for (ScheduleSlot neighbor : graph.getNeighbors(nextVertex)) {
                if (!coloring.containsKey(neighbor)) {
                    saturation.get(neighbor).add(color);
                }
            }
        }

        int maxColor = coloring.values().stream()
                .max(Integer::compareTo)
                .orElse(0);
        log.info("DSATUR colored with {} colors", maxColor + 1);

        return coloring;
    }

    private ScheduleSlot selectNextVertex(ConflictGraph graph,
            Map<ScheduleSlot, Integer> coloring,
            Map<ScheduleSlot, Set<Integer>> saturation) {
        ScheduleSlot selected = null;
        int maxSaturation = -1;
        int maxDegree = -1;

        for (ScheduleSlot vertex : graph.getVertices()) {
            if (coloring.containsKey(vertex))
                continue;

            int sat = saturation.get(vertex).size();
            int deg = graph.getDegree(vertex);

            if (sat > maxSaturation || (sat == maxSaturation && deg > maxDegree)) {
                selected = vertex;
                maxSaturation = sat;
                maxDegree = deg;
            }
        }

        return selected;
    }

    private int findSmallestColor(ScheduleSlot vertex, ConflictGraph graph,
            Map<ScheduleSlot, Integer> coloring) {
        Set<Integer> usedColors = new HashSet<>();

        for (ScheduleSlot neighbor : graph.getNeighbors(vertex)) {
            if (coloring.containsKey(neighbor)) {
                usedColors.add(coloring.get(neighbor));
            }
        }

        int color = 0;
        while (usedColors.contains(color)) {
            color++;
        }

        return color;
    }
}

// ========================================================================
// ADAPTIVE CHAOS OPTIMIZATION
// ========================================================================

class ChaosOptimizer {
    private final double[] chaosParameters = { 3.57, 3.82, 3.99 }; // Different chaos levels
    private int iteration = 0;

    public Schedule optimizeWithChaos(Schedule initial) {
        Schedule best = initial;
        double bestFitness = evaluateFitness(best);

        for (double r : chaosParameters) {
            Schedule chaotic = applyChaosIteration(initial, r);
            double fitness = evaluateFitness(chaotic);

            if (fitness < bestFitness) {
                best = chaotic;
                bestFitness = fitness;
            }
        }

        return best;
    }

    private Schedule applyChaosIteration(Schedule schedule, double r) {
        double x = 0.1;
        Schedule current = schedule.copy();

        for (int i = 0; i < 100; i++) {
            // Logistic map
            x = r * x * (1 - x);

            // Apply chaotic mutation based on x value
            if (x > 0.7) {
                current = strongMutation(current);
            } else if (x > 0.4) {
                current = mediumMutation(current);
            } else if (x > 0.1) {
                current = weakMutation(current);
            }
        }

        return current;
    }

    private Schedule strongMutation(Schedule schedule) {
        // Major changes: swap multiple slots
        List<ScheduleSlot> slots = schedule.getSlots();
        if (slots == null || slots.size() < 4) return schedule;

        Random random = new Random();
        // Swap 3-5 random pairs of slots
        int swapCount = 3 + random.nextInt(3);
        for (int i = 0; i < swapCount; i++) {
            int idx1 = random.nextInt(slots.size());
            int idx2 = random.nextInt(slots.size());
            if (idx1 != idx2) {
                ScheduleSlot s1 = slots.get(idx1);
                ScheduleSlot s2 = slots.get(idx2);
                // Swap time slots
                java.time.DayOfWeek tempDay = s1.getDayOfWeek();
                java.time.LocalTime tempStart = s1.getStartTime();
                s1.setDayOfWeek(s2.getDayOfWeek());
                s1.setStartTime(s2.getStartTime());
                s2.setDayOfWeek(tempDay);
                s2.setStartTime(tempStart);
            }
        }
        return schedule;
    }

    private Schedule mediumMutation(Schedule schedule) {
        // Moderate changes: swap teachers or rooms for 1-2 slots
        List<ScheduleSlot> slots = schedule.getSlots();
        if (slots == null || slots.size() < 2) return schedule;

        Random random = new Random();
        int swapCount = 1 + random.nextInt(2);
        for (int i = 0; i < swapCount; i++) {
            int idx1 = random.nextInt(slots.size());
            int idx2 = random.nextInt(slots.size());
            if (idx1 != idx2) {
                ScheduleSlot s1 = slots.get(idx1);
                ScheduleSlot s2 = slots.get(idx2);
                // Swap rooms
                Room tempRoom = s1.getRoom();
                s1.setRoom(s2.getRoom());
                s2.setRoom(tempRoom);
            }
        }
        return schedule;
    }

    private Schedule weakMutation(Schedule schedule) {
        // Minor changes: adjust time of single slot by +/- 1 hour
        List<ScheduleSlot> slots = schedule.getSlots();
        if (slots == null || slots.isEmpty()) return schedule;

        Random random = new Random();
        ScheduleSlot slot = slots.get(random.nextInt(slots.size()));
        if (slot.getStartTime() != null) {
            int hourAdjust = random.nextBoolean() ? 1 : -1;
            java.time.LocalTime newTime = slot.getStartTime().plusHours(hourAdjust);
            // Keep within school hours (7:00 - 16:00)
            if (newTime.getHour() >= 7 && newTime.getHour() <= 15) {
                slot.setStartTime(newTime);
                slot.setEndTime(newTime.plusMinutes(50));
            }
        }
        return schedule;
    }

    private double evaluateFitness(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null) {
            return Double.MAX_VALUE; // Worst fitness
        }

        double fitness = 0.0;

        // Penalty for conflicts (heavily weighted)
        int teacherConflicts = 0;
        int roomConflicts = 0;
        Map<String, Integer> teacherTimeCount = new HashMap<>();
        Map<String, Integer> roomTimeCount = new HashMap<>();

        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getTeacher() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getTeacher().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                int count = teacherTimeCount.merge(key, 1, Integer::sum);
                if (count > 1) teacherConflicts++;
            }
            if (slot.getRoom() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getRoom().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                int count = roomTimeCount.merge(key, 1, Integer::sum);
                if (count > 1) roomConflicts++;
            }
        }

        fitness += teacherConflicts * 100; // Heavy penalty for teacher conflicts
        fitness += roomConflicts * 50;     // Moderate penalty for room conflicts

        // Penalty for unassigned slots
        long unassigned = schedule.getSlots().stream()
            .filter(s -> s.getTeacher() == null || s.getRoom() == null)
            .count();
        fitness += unassigned * 25;

        // Reward for balanced teacher workload (subtract from fitness)
        Map<Long, Long> teacherCounts = schedule.getSlots().stream()
            .filter(s -> s.getTeacher() != null)
            .collect(Collectors.groupingBy(s -> s.getTeacher().getId(), Collectors.counting()));
        if (!teacherCounts.isEmpty()) {
            double mean = teacherCounts.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = teacherCounts.values().stream()
                .mapToDouble(c -> Math.pow(c - mean, 2)).average().orElse(0);
            fitness += Math.sqrt(variance) * 5; // Penalty for imbalance
        }

        return fitness; // Lower is better
    }
}

// ========================================================================
// HARMONY SEARCH ALGORITHM
// ========================================================================

class HarmonySearch {
    private final int memorySize = 20;
    private final double HMCR = 0.85; // Harmony Memory Considering Rate
    private final double PAR = 0.35; // Pitch Adjusting Rate
    private final double bandwidth = 0.1;

    private List<Schedule> harmonyMemory;

    public Schedule optimize(Schedule initial, int iterations) {
        // Initialize harmony memory
        harmonyMemory = new ArrayList<>();
        for (int i = 0; i < memorySize; i++) {
            harmonyMemory.add(generateRandomSchedule(initial));
        }

        // Sort by fitness
        harmonyMemory.sort(Comparator.comparing(this::evaluateFitness));

        for (int iter = 0; iter < iterations; iter++) {
            // Generate new harmony
            Schedule newHarmony = generateNewHarmony();

            // Evaluate fitness
            double fitness = evaluateFitness(newHarmony);

            // Update harmony memory if better
            if (fitness < evaluateFitness(harmonyMemory.get(memorySize - 1))) {
                harmonyMemory.set(memorySize - 1, newHarmony);
                harmonyMemory.sort(Comparator.comparing(this::evaluateFitness));
            }
        }

        return harmonyMemory.get(0); // Return best
    }

    private Schedule generateNewHarmony() {
        Schedule newSchedule = new Schedule();
        Random random = new Random();

        // For each decision variable
        for (ScheduleSlot slot : newSchedule.getSlots()) {
            if (random.nextDouble() < HMCR) {
                // Choose from harmony memory
                Schedule selected = harmonyMemory.get(
                        random.nextInt(memorySize));
                copySlotAssignment(slot, selected);

                if (random.nextDouble() < PAR) {
                    // Pitch adjustment
                    adjustPitch(slot);
                }
            } else {
                // Random selection
                randomizeSlot(slot);
            }
        }

        return newSchedule;
    }

    private void copySlotAssignment(ScheduleSlot target, Schedule source) {
        // Copy assignment from source schedule's corresponding slot
        if (source == null || source.getSlots() == null || source.getSlots().isEmpty()) {
            return;
        }
        // Find a random slot from source to copy time assignment
        Random random = new Random();
        ScheduleSlot sourceSlot = source.getSlots().get(random.nextInt(source.getSlots().size()));
        target.setDayOfWeek(sourceSlot.getDayOfWeek());
        target.setStartTime(sourceSlot.getStartTime());
        target.setEndTime(sourceSlot.getEndTime());
    }

    private void adjustPitch(ScheduleSlot slot) {
        // Make small adjustments to the slot time (+/- 1 hour or adjacent day)
        Random random = new Random();
        if (random.nextBoolean() && slot.getStartTime() != null) {
            // Adjust time by +/- 1 hour
            int hourAdjust = random.nextBoolean() ? 1 : -1;
            java.time.LocalTime newTime = slot.getStartTime().plusHours(hourAdjust);
            if (newTime.getHour() >= 7 && newTime.getHour() <= 15) {
                slot.setStartTime(newTime);
                slot.setEndTime(newTime.plusMinutes(50));
            }
        } else if (slot.getDayOfWeek() != null) {
            // Adjust day by +/- 1
            int dayValue = slot.getDayOfWeek().getValue();
            int newDay = dayValue + (random.nextBoolean() ? 1 : -1);
            if (newDay >= 1 && newDay <= 5) { // Mon-Fri
                slot.setDayOfWeek(java.time.DayOfWeek.of(newDay));
            }
        }
    }

    private void randomizeSlot(ScheduleSlot slot) {
        // Randomly assign slot time values
        Random random = new Random();
        // Random day (Mon-Fri)
        slot.setDayOfWeek(java.time.DayOfWeek.of(1 + random.nextInt(5)));
        // Random hour (8 AM - 3 PM)
        int hour = 8 + random.nextInt(7);
        int minute = random.nextInt(2) * 30; // 0 or 30
        slot.setStartTime(java.time.LocalTime.of(hour, minute));
        slot.setEndTime(slot.getStartTime().plusMinutes(50));
    }

    private Schedule generateRandomSchedule(Schedule template) {
        // Generate random variation of template
        Schedule copy = template.copy();
        Random random = new Random();

        // Randomize 20-50% of slots
        if (copy.getSlots() != null) {
            double changeRate = 0.2 + random.nextDouble() * 0.3;
            for (ScheduleSlot slot : copy.getSlots()) {
                if (random.nextDouble() < changeRate) {
                    randomizeSlot(slot);
                }
            }
        }
        return copy;
    }

    private double evaluateFitness(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null) {
            return Double.MAX_VALUE;
        }

        double fitness = 0.0;

        // Count conflicts
        Map<String, Integer> teacherTimeCount = new HashMap<>();
        Map<String, Integer> roomTimeCount = new HashMap<>();

        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getTeacher() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getTeacher().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                int count = teacherTimeCount.merge(key, 1, Integer::sum);
                if (count > 1) fitness += 100; // Teacher conflict penalty
            }
            if (slot.getRoom() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String key = slot.getRoom().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
                int count = roomTimeCount.merge(key, 1, Integer::sum);
                if (count > 1) fitness += 50; // Room conflict penalty
            }
        }

        // Penalty for unassigned resources
        long unassigned = schedule.getSlots().stream()
            .filter(s -> s.getTeacher() == null || s.getRoom() == null)
            .count();
        fitness += unassigned * 25;

        return fitness; // Lower is better
    }
}

/**
 * Conflict Graph for Schedule Slot Graph Coloring
 *
 * Represents a graph where:
 * - Vertices are ScheduleSlots
 * - Edges connect slots that have conflicts (same teacher/room at overlapping times)
 *
 * Used by Welsh-Powell and DSATUR graph coloring algorithms
 * to assign time slots (colors) that minimize conflicts.
 */
class ConflictGraph {
    private final Set<ScheduleSlot> vertices;
    private final Map<ScheduleSlot, Set<ScheduleSlot>> adjacencyList;

    public ConflictGraph() {
        this.vertices = new HashSet<>();
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Build conflict graph from schedule slots
     * Two slots conflict if they share the same teacher or room
     * and have overlapping times on the same day.
     */
    public static ConflictGraph fromScheduleSlots(List<ScheduleSlot> slots) {
        ConflictGraph graph = new ConflictGraph();

        if (slots == null || slots.isEmpty()) {
            return graph;
        }

        // Add all slots as vertices
        for (ScheduleSlot slot : slots) {
            graph.addVertex(slot);
        }

        // Check all pairs for conflicts
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ScheduleSlot slot1 = slots.get(i);
                ScheduleSlot slot2 = slots.get(j);

                if (hasConflict(slot1, slot2)) {
                    graph.addEdge(slot1, slot2);
                }
            }
        }

        return graph;
    }

    /**
     * Check if two slots have a conflict
     */
    private static boolean hasConflict(ScheduleSlot slot1, ScheduleSlot slot2) {
        // Same slot doesn't conflict with itself
        if (slot1.equals(slot2)) {
            return false;
        }

        // Check for time overlap on same day
        if (!timeOverlaps(slot1, slot2)) {
            return false;
        }

        // Check for teacher conflict
        if (slot1.getTeacher() != null && slot2.getTeacher() != null &&
            slot1.getTeacher().getId().equals(slot2.getTeacher().getId())) {
            return true;
        }

        // Check for room conflict
        if (slot1.getRoom() != null && slot2.getRoom() != null &&
            slot1.getRoom().getId().equals(slot2.getRoom().getId())) {
            return true;
        }

        return false;
    }

    /**
     * Check if two slots have overlapping time on the same day
     */
    private static boolean timeOverlaps(ScheduleSlot slot1, ScheduleSlot slot2) {
        if (slot1.getDayOfWeek() == null || slot2.getDayOfWeek() == null) {
            return false;
        }

        // Must be same day
        if (!slot1.getDayOfWeek().equals(slot2.getDayOfWeek())) {
            return false;
        }

        // Check time overlap
        java.time.LocalTime start1 = slot1.getStartTime();
        java.time.LocalTime end1 = slot1.getEndTime();
        java.time.LocalTime start2 = slot2.getStartTime();
        java.time.LocalTime end2 = slot2.getEndTime();

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        // Overlap: start1 < end2 AND start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public void addVertex(ScheduleSlot vertex) {
        vertices.add(vertex);
        adjacencyList.putIfAbsent(vertex, new HashSet<>());
    }

    public void addEdge(ScheduleSlot v1, ScheduleSlot v2) {
        addVertex(v1);
        addVertex(v2);
        adjacencyList.get(v1).add(v2);
        adjacencyList.get(v2).add(v1);
    }

    public Set<ScheduleSlot> getVertices() {
        return new HashSet<>(vertices);
    }

    public Set<ScheduleSlot> getNeighbors(ScheduleSlot slot) {
        return adjacencyList.getOrDefault(slot, new HashSet<>());
    }

    public int getDegree(ScheduleSlot slot) {
        return adjacencyList.getOrDefault(slot, new HashSet<>()).size();
    }

    /**
     * Get total number of edges (conflicts) in the graph
     */
    public int getEdgeCount() {
        int totalDegree = adjacencyList.values().stream()
            .mapToInt(Set::size)
            .sum();
        return totalDegree / 2; // Each edge counted twice
    }

    /**
     * Check if graph has any conflicts
     */
    public boolean hasConflicts() {
        return getEdgeCount() > 0;
    }
}