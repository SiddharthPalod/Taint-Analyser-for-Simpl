package analysis;

import cfg.*;
import java.util.*;

/**
 * Taint Analysis using Kildall Worklist Algorithm
 * Implements the fixed-point solver for data-flow analysis
 */
public class TaintAnalyzer {
    protected final cfg.CFGBuilder.CFGResult cfg;
    protected final Map<BasicBlock, AnalysisState> IN;
    protected final Map<BasicBlock, AnalysisState> OUT;
    protected final Set<String> allVars;
    
    public TaintAnalyzer(cfg.CFGBuilder.CFGResult cfg, Set<String> allVars) {
        this.cfg = cfg;
        this.allVars = allVars;
        this.IN = new HashMap<>();
        this.OUT = new HashMap<>();
        
        // Initialize all blocks to bottom state
        for (BasicBlock block : cfg.allBlocks) {
            IN.put(block, AnalysisState.getBottomState(allVars));
            OUT.put(block, AnalysisState.getBottomState(allVars));
        }
    }
    
    /**
     * Main analysis method using Kildall Worklist Algorithm
     */
    public void analyze() {
        Queue<BasicBlock> worklist = new LinkedList<>();
        
        // Initialize worklist with all blocks (forward analysis)
        worklist.addAll(cfg.allBlocks);
        
        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.poll();
            
            // 1. Merge (Join) from predecessors (IN[n] = join of all OUT[p] for predecessors p)
            AnalysisState oldIn = IN.get(block);
            AnalysisState newIn = computeInState(block);
            IN.put(block, newIn);
            
            // 2. Transfer Function (OUT[n] = F_n(IN[n]))
            AnalysisState oldOut = OUT.get(block);
            AnalysisState newOut = transferFunction(block, newIn);
            OUT.put(block, newOut);
            
            // 3. Update Worklist if IN or OUT changed
            boolean inChanged = !newIn.equals(oldIn);
            boolean outChanged = !newOut.equals(oldOut);
            
            if (inChanged || outChanged) {
                for (BasicBlock successor : block.getSuccessors()) {
                    if (!worklist.contains(successor)) {
                        worklist.add(successor);
                    }
                }
            }
        }
    }
    
    /**
     * Compute IN state by joining all predecessor OUT states
     */
    protected AnalysisState computeInState(BasicBlock block) {
        List<BasicBlock> predecessors = block.getPredecessors();
        
        if (predecessors.isEmpty()) {
            // Entry block - start with bottom state
            return AnalysisState.getBottomState(allVars);
        }
        
        // Join all predecessor OUT states
        AnalysisState result = OUT.get(predecessors.get(0));
        for (int i = 1; i < predecessors.size(); i++) {
            result = result.join(OUT.get(predecessors.get(i)));
        }
        
        // If this is a merge block (multiple predecessors), clear control taint
        // because we're exiting the conditional context
        if (predecessors.size() > 1) {
            result = result.clearControlOrigins();
        }
        
        return result;
    }
    
    /**
     * Transfer Function F_n: IN[n] -> OUT[n]
     * Implements the core taint propagation logic including implicit flow
     */
    protected AnalysisState transferFunction(BasicBlock block, AnalysisState inState) {
        AnalysisState state = inState;
        
        for (BasicBlock.Statement stmt : block.statements) {
            state = applyStatement(block, stmt, state);
        }
        
        return state;
    }
    
    /**
     * Apply a single statement to the analysis state
     */
    protected AnalysisState applyStatement(BasicBlock block, BasicBlock.Statement stmt, AnalysisState state) {
        switch (stmt.type) {
            case ASSIGN_INPUT:
                // x = inputExpr() -> x becomes T (tainted)
                return state.setVarTaint(stmt.var, AnalysisState.TaintState.T);
                
            case ASSIGN:
                // x = y -> x gets taint of y OR control taint
                AnalysisState.TaintState yTaint = state.getVarTaint(stmt.rightVar);
                // Implicit flow: if control is tainted, assignment is tainted
                AnalysisState.TaintState finalTaint = (yTaint == AnalysisState.TaintState.T || 
                                                       state.isControlTainted())
                    ? AnalysisState.TaintState.T : AnalysisState.TaintState.NT;
                return state.setVarTaint(stmt.var, finalTaint);
                
            case ASSIGN_BIN:
                // x = y op z -> x gets join of y and z taints OR control taint
                AnalysisState.TaintState leftTaint = state.getVarTaint(stmt.leftVar);
                AnalysisState.TaintState rightTaint = state.getVarTaint(stmt.rightVar);
                AnalysisState.TaintState binTaint = (leftTaint == AnalysisState.TaintState.T || 
                                                     rightTaint == AnalysisState.TaintState.T ||
                                                     state.isControlTainted())
                    ? AnalysisState.TaintState.T : AnalysisState.TaintState.NT;
                return state.setVarTaint(stmt.var, binTaint);
                
            case COND:
                // if/while condition -> if condition variable is tainted, taint control
                // Extract variable from condition (simplified - assumes condition is a variable comparison)
                String condVar = extractConditionVar(stmt.condition);
                if (condVar != null) {
                    AnalysisState.TaintState condVarTaint = state.getVarTaint(condVar);
                    if (condVarTaint == AnalysisState.TaintState.T) {
                        state = state.addControlOrigin(block);
                    }
                }
                return state;
                
            case SINK:
                // sinkExpr(x) - check for leak (will be reported separately)
                // No state change, just mark for reporting
                return state;
                
            default:
                return state;
        }
    }
    
    /**
     * Extract variable name from condition expression
     * Simplified: assumes conditions are comparisons with variables
     */
    protected String extractConditionVar(Object condition) {
        if (condition == null) return null;
        // This is a simplified version - in practice, you'd need to handle
        // complex conditions. For now, we'll check if it's a binary expression
        // with an ID on the left
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
    
    /**
     * Get the OUT state for a block (for reporting)
     */
    public AnalysisState getOutState(BasicBlock block) {
        return OUT.get(block);
    }
    
    /**
     * Get the IN state for a block
     */
    public AnalysisState getInState(BasicBlock block) {
        return IN.get(block);
    }
    
    /**
     * Report leaks: find all sinkExpr statements and check if their arguments are tainted
     */
    public List<LeakReport> reportLeaks() {
        List<LeakReport> leaks = new ArrayList<>();
        
        for (BasicBlock block : cfg.allBlocks) {
            // Start with IN state and simulate up to each SINK statement
            AnalysisState currentState = IN.get(block);
            
            for (BasicBlock.Statement stmt : block.statements) {
                if (stmt.type == BasicBlock.Statement.Type.SINK) {
                    // Check state right before this SINK statement
                    AnalysisState.TaintState varTaint = currentState.getVarTaint(stmt.var);
                    if (varTaint == AnalysisState.TaintState.T) {
                        leaks.add(new LeakReport(stmt.lineNumber, stmt.var, "sinkExpr"));
                    }
                } else {
                    // Apply statement to get state for next SINK check
                    currentState = applyStatement(block, stmt, currentState);
                }
            }
        }
        
        return leaks;
    }
    
    /**
     * Leak report structure
     */
    public static class LeakReport {
        public final int lineNumber;
        public final String taintedVariable;
        public final String sink;
        
        public LeakReport(int lineNumber, String taintedVariable, String sink) {
            this.lineNumber = lineNumber;
            this.taintedVariable = taintedVariable;
            this.sink = sink;
        }
        
        @Override
        public String toString() {
            return String.format("LEAK at line %d: variable '%s' is tainted at %s", 
                lineNumber, taintedVariable, sink);
        }
    }
}

