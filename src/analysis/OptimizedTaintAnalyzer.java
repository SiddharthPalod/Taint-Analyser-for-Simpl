package analysis;

import cfg.BasicBlock;
import cfg.CFGBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optimized Taint Analyzer that leverages the Post-Dominator Tree
 * to reset control taint exactly where control scopes merge.
 */
public class OptimizedTaintAnalyzer extends TaintAnalyzer {
    private final PostDominatorTree postDominatorTree;

    public OptimizedTaintAnalyzer(CFGBuilder.CFGResult cfg, Set<String> allVars) {
        super(cfg, allVars);
        this.postDominatorTree = new PostDominatorTree(cfg);
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

