package analysis;

import cfg.BasicBlock;

import java.util.*;

/**
 * Analysis State: tracks variable taints and the set of active control-origin blocks.
 */
public class AnalysisState {
    public enum TaintState { NT, T }

    public final Map<String, TaintState> varMap;
    public final Set<BasicBlock> controlOrigins;

    public AnalysisState(Map<String, TaintState> varMap, Set<BasicBlock> controlOrigins) {
        this.varMap = new HashMap<>(varMap);
        this.controlOrigins = new HashSet<>(controlOrigins);
    }

    public static AnalysisState getBottomState(Set<String> allVars) {
        Map<String, TaintState> map = new HashMap<>();
        for (String var : allVars) {
            map.put(var, TaintState.NT);
        }
        return new AnalysisState(map, Collections.emptySet());
    }

    public AnalysisState join(AnalysisState other) {
        Map<String, TaintState> newMap = new HashMap<>(this.varMap);
        for (Map.Entry<String, TaintState> entry : other.varMap.entrySet()) {
            TaintState thisT = newMap.getOrDefault(entry.getKey(), TaintState.NT);
            TaintState otherT = entry.getValue();
            newMap.put(entry.getKey(), joinTaint(thisT, otherT));
        }

        for (Map.Entry<String, TaintState> entry : this.varMap.entrySet()) {
            if (!other.varMap.containsKey(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }

        Set<BasicBlock> newOrigins = new HashSet<>(this.controlOrigins);
        newOrigins.addAll(other.controlOrigins);

        return new AnalysisState(newMap, newOrigins);
    }

    private TaintState joinTaint(TaintState t1, TaintState t2) {
        return (t1 == TaintState.T || t2 == TaintState.T) ? TaintState.T : TaintState.NT;
    }

    public TaintState getVarTaint(String var) {
        return varMap.getOrDefault(var, TaintState.NT);
    }

    public AnalysisState setVarTaint(String var, TaintState taint) {
        Map<String, TaintState> newMap = new HashMap<>(this.varMap);
        newMap.put(var, taint);
        return new AnalysisState(newMap, this.controlOrigins);
    }

    public boolean isControlTainted() {
        return !controlOrigins.isEmpty();
    }

    public AnalysisState addControlOrigin(BasicBlock origin) {
        Set<BasicBlock> newOrigins = new HashSet<>(this.controlOrigins);
        newOrigins.add(origin);
        return new AnalysisState(this.varMap, newOrigins);
    }

    public AnalysisState clearControlOrigins() {
        return new AnalysisState(this.varMap, Collections.emptySet());
    }

    public AnalysisState removeControlOrigins(Collection<BasicBlock> origins) {
        if (origins.isEmpty()) return this;
        Set<BasicBlock> newOrigins = new HashSet<>(this.controlOrigins);
        newOrigins.removeAll(origins);
        return new AnalysisState(this.varMap, newOrigins);
    }

    public Set<BasicBlock> getControlOrigins() {
        return Collections.unmodifiableSet(controlOrigins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisState that = (AnalysisState) o;
        return Objects.equals(varMap, that.varMap) &&
               Objects.equals(controlOrigins, that.controlOrigins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varMap, controlOrigins);
    }

    @Override
    public String toString() {
        return "AnalysisState{" +
                "vars=" + varMap +
                ", controlOrigins=" + controlOrigins +
                '}';
    }
}

