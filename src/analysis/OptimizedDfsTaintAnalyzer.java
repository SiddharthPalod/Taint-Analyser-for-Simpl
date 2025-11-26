package analysis;

import cfg.BasicBlock;
import cfg.CFGBuilder;
import pdt.BackpropPostDominatorTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Variant of the optimized taint analyzer that relies on the DFS/back-propagation
 * post-dominator tree builder to reset control-taint exactly at merge points.
 */
public class OptimizedDfsTaintAnalyzer extends TaintAnalyzer {
    private final BackpropPostDominatorTree postDominatorTree;

    public OptimizedDfsTaintAnalyzer(CFGBuilder.CFGResult cfg, Set<String> allVars) {
        super(cfg, allVars);
        this.postDominatorTree = new BackpropPostDominatorTree(cfg);
    }

    @Override
    protected AnalysisState computeInState(BasicBlock block) {
        List<BasicBlock> predecessors = block.getPredecessors();

        if (predecessors.isEmpty()) {
            return AnalysisState.getBottomState(allVars);
        }

        AnalysisState result = getOutState(predecessors.get(0));
        for (int i = 1; i < predecessors.size(); i++) {
            result = result.join(getOutState(predecessors.get(i)));
        }

        result = removeCompletedControlOrigins(block, result);

        return result;
    }

    private AnalysisState removeCompletedControlOrigins(BasicBlock block, AnalysisState state) {
        Set<BasicBlock> toRemove = new HashSet<>();
        for (BasicBlock origin : state.getControlOrigins()) {
            BasicBlock immediate = postDominatorTree.getImmediate(origin);
            if (immediate != null && immediate == block) {
                toRemove.add(origin);
            }
        }
        return state.removeControlOrigins(toRemove);
    }
}

