package analysis;

import cfg.*;
import java.util.*;

/**
 * Optimized Taint Analyzer with Post-Dominator Tree optimization
 * This version resets control taint at post-dominator merge points
 * to reduce false positives
 */
public class OptimizedTaintAnalyzer extends TaintAnalyzer {
    private final Map<BasicBlock, BasicBlock> postDominators;
    
    public OptimizedTaintAnalyzer(cfg.CFGBuilder.CFGResult cfg, Set<String> allVars) {
        super(cfg, allVars);
        this.postDominators = computePostDominators(cfg);
    }
    
    /**
     * Compute post-dominators for all blocks
     * A node D post-dominates N if every path from N to exit must pass through D
     */
    private Map<BasicBlock, BasicBlock> computePostDominators(cfg.CFGBuilder.CFGResult cfg) {
        Map<BasicBlock, BasicBlock> pdom = new HashMap<>();
        Map<BasicBlock, Set<BasicBlock>> pdomSets = new HashMap<>();
        
        // Initialize: all blocks post-dominate themselves
        for (BasicBlock block : cfg.allBlocks) {
            Set<BasicBlock> set = new HashSet<>();
            set.add(block);
            pdomSets.put(block, set);
        }
        
        // Iterative algorithm to compute post-dominators
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (BasicBlock block : cfg.allBlocks) {
                Set<BasicBlock> newSet = new HashSet<>();
                newSet.add(block);
                
                // Intersect post-dominators of all successors
                List<BasicBlock> successors = block.getSuccessors();
                if (!successors.isEmpty()) {
                    // Start with first successor's post-dominators
                    newSet.addAll(pdomSets.get(successors.get(0)));
                    
                    // Intersect with all other successors
                    for (int i = 1; i < successors.size(); i++) {
                        newSet.retainAll(pdomSets.get(successors.get(i)));
                    }
                }
                
                // Add self
                newSet.add(block);
                
                if (!newSet.equals(pdomSets.get(block))) {
                    pdomSets.put(block, newSet);
                    changed = true;
                }
            }
        }
        
        // Find immediate post-dominator (closest post-dominator that is not self)
        for (BasicBlock block : cfg.allBlocks) {
            Set<BasicBlock> domSet = pdomSets.get(block);
            BasicBlock immediatePDom = null;
            
            // Find the closest post-dominator (one that is post-dominated by all others)
            for (BasicBlock candidate : domSet) {
                if (candidate == block) continue;
                
                boolean isImmediate = true;
                for (BasicBlock other : domSet) {
                    if (other == block || other == candidate) continue;
                    // Check if other is on path from block to candidate
                    if (isReachable(block, candidate, other, cfg)) {
                        isImmediate = false;
                        break;
                    }
                }
                
                if (isImmediate) {
                    immediatePDom = candidate;
                    break;
                }
            }
            
            pdom.put(block, immediatePDom);
        }
        
        return pdom;
    }
    
    /**
     * Check if 'middle' is on a path from 'start' to 'end'
     */
    private boolean isReachable(BasicBlock start, BasicBlock end, BasicBlock middle, 
                                cfg.CFGBuilder.CFGResult cfg) {
        Set<BasicBlock> visited = new HashSet<>();
        return dfsReachable(start, end, middle, visited);
    }
    
    private boolean dfsReachable(BasicBlock current, BasicBlock target, BasicBlock mustVisit,
                                 Set<BasicBlock> visited) {
        if (current == target) {
            return visited.contains(mustVisit);
        }
        if (visited.contains(current)) {
            return false;
        }
        visited.add(current);
        
        for (BasicBlock succ : current.getSuccessors()) {
            if (dfsReachable(succ, target, mustVisit, visited)) {
                return true;
            }
        }
        
        visited.remove(current);
        return false;
    }
    
    /**
     * Override transfer function to reset control taint at post-dominator merge points
     * Note: We need to override the analyze() method to use our custom transfer function
     */
    @Override
    public void analyze() {
        Queue<BasicBlock> worklist = new LinkedList<>();
        cfg.CFGBuilder.CFGResult cfg = getCFG();
        
        // Initialize worklist with all blocks
        worklist.addAll(cfg.allBlocks);
        
        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.poll();
            
            // 1. Merge from predecessors
            AnalysisState oldIn = getInState(block);
            AnalysisState newIn = computeInStateOptimized(block);
            setInState(block, newIn);
            
            if (newIn.equals(oldIn)) {
                continue;
            }
            
            // 2. Transfer Function with post-dominator optimization
            AnalysisState oldOut = getOutState(block);
            AnalysisState newOut = transferFunctionOptimized(block, newIn);
            setOutState(block, newOut);
            
            // 3. Update worklist if OUT changed
            if (!newOut.equals(oldOut)) {
                for (BasicBlock successor : block.getSuccessors()) {
                    if (!worklist.contains(successor)) {
                        worklist.add(successor);
                    }
                }
            }
        }
    }
    
    private cfg.CFGBuilder.CFGResult getCFG() {
        // Access protected field through reflection or make it accessible
        // For now, we'll need to store it
        try {
            java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("cfg");
            field.setAccessible(true);
            return (cfg.CFGBuilder.CFGResult) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public AnalysisState getInState(BasicBlock block) {
        try {
            java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("IN");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<BasicBlock, AnalysisState> IN = (Map<BasicBlock, AnalysisState>) field.get(this);
            return IN.get(block);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void setInState(BasicBlock block, AnalysisState state) {
        try {
            java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("IN");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<BasicBlock, AnalysisState> IN = (Map<BasicBlock, AnalysisState>) field.get(this);
            IN.put(block, state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public AnalysisState getOutState(BasicBlock block) {
        try {
            java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("OUT");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<BasicBlock, AnalysisState> OUT = (Map<BasicBlock, AnalysisState>) field.get(this);
            return OUT.get(block);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void setOutState(BasicBlock block, AnalysisState state) {
        try {
            java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("OUT");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<BasicBlock, AnalysisState> OUT = (Map<BasicBlock, AnalysisState>) field.get(this);
            OUT.put(block, state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private AnalysisState computeInStateOptimized(BasicBlock block) {
        List<BasicBlock> predecessors = block.getPredecessors();
        
        if (predecessors.isEmpty()) {
            try {
                java.lang.reflect.Field field = TaintAnalyzer.class.getDeclaredField("allVars");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Set<String> allVars = (Set<String>) field.get(this);
                return AnalysisState.getBottomState(allVars);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        AnalysisState result = getOutState(predecessors.get(0));
        for (int i = 1; i < predecessors.size(); i++) {
            result = result.join(getOutState(predecessors.get(i)));
        }
        
        // Post-dominator optimization: reset control taint at merge points
        if (predecessors.size() > 1) {
            BasicBlock pdom = postDominators.get(block);
            if (pdom != null && pdom != block) {
                AnalysisState pdomState = getOutState(pdom);
                if (pdomState != null) {
                    result = result.setControlTaint(pdomState.controlTaint);
                }
            }
        }
        
        return result;
    }
    
    private AnalysisState transferFunctionOptimized(BasicBlock block, AnalysisState inState) {
        // Use the parent's transfer function logic
        // We'll call it through reflection or duplicate the logic
        // For simplicity, let's duplicate and modify
        AnalysisState state = inState;
        
        for (BasicBlock.Statement stmt : block.statements) {
            state = applyStatementOptimized(stmt, state, block);
        }
        
        return state;
    }
    
    private AnalysisState applyStatementOptimized(BasicBlock.Statement stmt, AnalysisState state, BasicBlock block) {
        // Same as parent, but we can add post-dominator logic here if needed
        switch (stmt.type) {
            case ASSIGN_INPUT:
                return state.setVarTaint(stmt.var, AnalysisState.TaintState.T);
                
            case ASSIGN:
                AnalysisState.TaintState yTaint = state.getVarTaint(stmt.rightVar);
                AnalysisState.TaintState finalTaint = (yTaint == AnalysisState.TaintState.T || 
                                                       state.controlTaint == AnalysisState.TaintState.T)
                    ? AnalysisState.TaintState.T : AnalysisState.TaintState.NT;
                return state.setVarTaint(stmt.var, finalTaint);
                
            case ASSIGN_BIN:
                AnalysisState.TaintState leftTaint = state.getVarTaint(stmt.leftVar);
                AnalysisState.TaintState rightTaint = state.getVarTaint(stmt.rightVar);
                AnalysisState.TaintState binTaint = (leftTaint == AnalysisState.TaintState.T || 
                                                     rightTaint == AnalysisState.TaintState.T ||
                                                     state.controlTaint == AnalysisState.TaintState.T)
                    ? AnalysisState.TaintState.T : AnalysisState.TaintState.NT;
                return state.setVarTaint(stmt.var, binTaint);
                
            case COND:
                String condVar = extractConditionVar(stmt.condition);
                if (condVar != null) {
                    AnalysisState.TaintState condVarTaint = state.getVarTaint(condVar);
                    if (condVarTaint == AnalysisState.TaintState.T) {
                        state = state.setControlTaint(AnalysisState.TaintState.T);
                    }
                }
                return state;
                
            case SINK:
                return state;
                
            default:
                return state;
        }
    }
    
    private String extractConditionVar(Object condition) {
        if (condition == null) return null;
        if (condition instanceof ast.BinExpr) {
            ast.BinExpr bin = (ast.BinExpr) condition;
            if (bin.left instanceof ast.Id) {
                return ((ast.Id) bin.left).name;
            }
        } else if (condition instanceof ast.Id) {
            return ((ast.Id) condition).name;
        }
        return null;
    }
}

