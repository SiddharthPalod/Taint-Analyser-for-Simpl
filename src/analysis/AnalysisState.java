package analysis;

import java.util.*;

/**
 * Analysis State: (Map<Var, TaintState>, ControlTaint)
 * Represents the lattice V = M × C where:
 * - M: Map from variables to taint states
 * - C: Control taint (T or NT)
 */
public class AnalysisState {
    public enum TaintState { NT, T }
    
    // The Map<Var, TaintState>
    public final Map<String, TaintState> varMap;
    // The Control Taint (C)
    public final TaintState controlTaint;
    
    public AnalysisState(Map<String, TaintState> varMap, TaintState controlTaint) {
        this.varMap = new HashMap<>(varMap);
        this.controlTaint = controlTaint;
    }
    
    /**
     * Constructor for the Bottom State (λv.NT, NT)
     */
    public static AnalysisState getBottomState(Set<String> allVars) {
        Map<String, TaintState> map = new HashMap<>();
        for (String var : allVars) {
            map.put(var, TaintState.NT);
        }
        return new AnalysisState(map, TaintState.NT);
    }
    
    /**
     * The Join Operation (⊔_V)
     * Joins two analysis states pointwise
     */
    public AnalysisState join(AnalysisState other) {
        // 1. Join Control Taint
        TaintState newC = (this.controlTaint == TaintState.T || other.controlTaint == TaintState.T) 
                            ? TaintState.T : TaintState.NT;
        
        // 2. Pointwise Join of Maps (⊔_M)
        Map<String, TaintState> newMap = new HashMap<>(this.varMap);
        for (Map.Entry<String, TaintState> entry : other.varMap.entrySet()) {
            TaintState thisT = newMap.getOrDefault(entry.getKey(), TaintState.NT);
            TaintState otherT = entry.getValue();
            newMap.put(entry.getKey(), joinTaint(thisT, otherT));
        }
        
        // Also include any variables from this state that aren't in other
        for (Map.Entry<String, TaintState> entry : this.varMap.entrySet()) {
            if (!other.varMap.containsKey(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        
        return new AnalysisState(newMap, newC);
    }
    
    private TaintState joinTaint(TaintState t1, TaintState t2) {
        return (t1 == TaintState.T || t2 == TaintState.T) ? TaintState.T : TaintState.NT;
    }
    
    /**
     * Get taint state of a variable
     */
    public TaintState getVarTaint(String var) {
        return varMap.getOrDefault(var, TaintState.NT);
    }
    
    /**
     * Set taint state of a variable
     */
    public AnalysisState setVarTaint(String var, TaintState taint) {
        Map<String, TaintState> newMap = new HashMap<>(this.varMap);
        newMap.put(var, taint);
        return new AnalysisState(newMap, this.controlTaint);
    }
    
    /**
     * Set control taint
     */
    public AnalysisState setControlTaint(TaintState taint) {
        return new AnalysisState(this.varMap, taint);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisState that = (AnalysisState) o;
        return Objects.equals(varMap, that.varMap) && controlTaint == that.controlTaint;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(varMap, controlTaint);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("vars: ").append(varMap);
        sb.append(", control: ").append(controlTaint);
        sb.append(")");
        return sb.toString();
    }
}

