package pdt;

import cfg.BasicBlock;
import cfg.CFGBuilder;

import java.util.*;

/**
 * Builds and stores the Post-Dominator Tree (PDT) for a CFG.
 * A node D post-dominates node N if every path from N to exit passes through D.
 */
public class PostDominatorTree {
    private final Map<BasicBlock, Set<BasicBlock>> postDominators = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> immediatePostDominators = new HashMap<>();

    public PostDominatorTree(CFGBuilder.CFGResult cfg) {
        computePostDominators(cfg);
        computeImmediatePostDominators(cfg);
    }

    /**
     * Returns the immediate post-dominator of the given block (or null if none).
     */
    public BasicBlock getImmediate(BasicBlock block) {
        return immediatePostDominators.get(block);
    }

    /**
     * Returns the full post-dominator set for the block.
     */
    public Set<BasicBlock> getPostDominators(BasicBlock block) {
        return postDominators.getOrDefault(block, Collections.emptySet());
    }

    private void computePostDominators(CFGBuilder.CFGResult cfg) {
        List<BasicBlock> allBlocks = cfg.allBlocks;
        BasicBlock exit = cfg.exit;
        Set<BasicBlock> universe = new HashSet<>(allBlocks);

        // Initialization
        for (BasicBlock block : allBlocks) {
            Set<BasicBlock> initial = new HashSet<>();
            if (block == exit) {
                initial.add(block);
            } else {
                initial.addAll(universe);
            }
            postDominators.put(block, initial);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock block : allBlocks) {
                if (block == exit) {
                    continue;
                }

                Set<BasicBlock> newSet = new HashSet<>();
                newSet.add(block);

                List<BasicBlock> successors = block.getSuccessors();
                if (!successors.isEmpty()) {
                    Set<BasicBlock> intersection = new HashSet<>(postDominators.get(successors.get(0)));
                    for (int i = 1; i < successors.size(); i++) {
                        intersection.retainAll(postDominators.get(successors.get(i)));
                    }
                    newSet.addAll(intersection);
                } else {
                    // Dead-end blocks post-dominate only themselves
                    newSet.add(block);
                }

                Set<BasicBlock> current = postDominators.get(block);
                if (!current.equals(newSet)) {
                    postDominators.put(block, newSet);
                    changed = true;
                }
            }
        }
    }

    private void computeImmediatePostDominators(CFGBuilder.CFGResult cfg) {
        for (BasicBlock block : cfg.allBlocks) {
            if (block == cfg.exit) {
                immediatePostDominators.put(block, null);
                continue;
            }

            Set<BasicBlock> candidates = new HashSet<>(postDominators.get(block));
            candidates.remove(block);

            BasicBlock immediate = null;
            for (BasicBlock candidate : candidates) {
                boolean dominatedByOther = false;
                for (BasicBlock other : candidates) {
                    if (other == candidate) {
                        continue;
                    }
                    if (postDominators.get(other).contains(candidate)) {
                        dominatedByOther = true;
                        break;
                    }
                }
                if (!dominatedByOther) {
                    immediate = candidate;
                    break;
                }
            }

            immediatePostDominators.put(block, immediate);
        }
    }
}