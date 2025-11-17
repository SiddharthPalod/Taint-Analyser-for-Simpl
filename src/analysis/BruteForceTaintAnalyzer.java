package analysis;

import cfg.BasicBlock;
import cfg.CFGBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Brute-force implementation of the taint analysis solver that
 * follows the textbook fixed-point iteration described in the
 * provided pseudocode. Instead of a worklist, it repeatedly scans
 * every block until no IN/OUT state changes are observed.
 */
public class BruteForceTaintAnalyzer extends TaintAnalyzer {

    public BruteForceTaintAnalyzer(CFGBuilder.CFGResult cfg, java.util.Set<String> allVars) {
        super(cfg, allVars);
    }

    @Override
    public void analyze() {
        Map<BasicBlock, AnalysisState> previousIn = new HashMap<>();
        Map<BasicBlock, AnalysisState> previousOut = new HashMap<>();

        boolean changed;
        do {
            snapshotStates(previousIn, previousOut);

            for (BasicBlock block : cfg.allBlocks) {
                AnalysisState newIn = computeInStateFromOutStates(block, previousOut);
                IN.put(block, newIn);

                AnalysisState newOut = transferFunction(block, newIn);
                OUT.put(block, newOut);
            }

            changed = !(statesEqual(previousIn, IN) && statesEqual(previousOut, OUT));
        } while (changed);
    }

    private void snapshotStates(Map<BasicBlock, AnalysisState> previousIn,
                                Map<BasicBlock, AnalysisState> previousOut) {
        previousIn.clear();
        previousOut.clear();

        for (BasicBlock block : cfg.allBlocks) {
            previousIn.put(block, IN.get(block));
            previousOut.put(block, OUT.get(block));
        }
    }

    private boolean statesEqual(Map<BasicBlock, AnalysisState> left,
                                Map<BasicBlock, AnalysisState> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (BasicBlock block : left.keySet()) {
            AnalysisState leftState = left.get(block);
            AnalysisState rightState = right.get(block);
            if (rightState == null || !leftState.equals(rightState)) {
                return false;
            }
        }
        return true;
    }
}


