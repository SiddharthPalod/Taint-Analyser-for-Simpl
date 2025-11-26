package pdt;

import cfg.BasicBlock;
import cfg.CFGBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Lengauer-Tarjan based post-dominator tree builder that operates on the
 * reversed CFG (back-propagation) to obtain immediate post-dominators, the
 * explicit tree structure, and LCA queries. The implementation is adapted
 * from "A Fast Algorithm for Finding Dominators in a Flowgraph" (Lengauer &
 * Tarjan, 1979) by treating the CFG exit as the root and traversing edges in
 * reverse.
 */
public class BackpropPostDominatorTree {
    private final List<BasicBlock> blocks;
    private final Map<BasicBlock, Integer> indexOf;
    private final Map<BasicBlock, BasicBlock> immediatePostDominators;
    private final List<List<Integer>> treeChildren;
    private final int[][] up;
    private final int[] depth;
    private final int[] tin;
    private final int[] tout;
    private final int log;
    private int timer = 0;
    private final BasicBlock exitBlock;

    public BackpropPostDominatorTree(CFGBuilder.CFGResult cfg) {
        this.blocks = cfg.allBlocks;
        this.exitBlock = cfg.exit;
        this.indexOf = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            indexOf.put(blocks.get(i), i);
        }
        int n = blocks.size();
        this.treeChildren = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            treeChildren.add(new ArrayList<>());
        }
        this.immediatePostDominators = new HashMap<>();
        this.depth = new int[n];
        this.tin = new int[n];
        this.tout = new int[n];
        int l = 1;
        while ((1 << l) <= Math.max(1, n)) {
            l++;
        }
        this.log = l;
        this.up = new int[n][log];
        build(cfg);
    }

    /**
     * Returns the immediate post-dominator of {@code block}, or {@code null} for the exit
     * node or blocks not connected to the exit.
     */
    public BasicBlock getImmediate(BasicBlock block) {
        return immediatePostDominators.get(block);
    }

    /**
     * Returns the immutable list of children of {@code block} in the PDT.
     */
    public List<BasicBlock> getChildren(BasicBlock block) {
        Integer idx = indexOf.get(block);
        if (idx == null) {
            return Collections.emptyList();
        }
        List<Integer> childIdx = treeChildren.get(idx);
        List<BasicBlock> result = new ArrayList<>(childIdx.size());
        for (int c : childIdx) {
            result.add(blocks.get(c));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Computes the least common ancestor of {@code a} and {@code b} in the PDT.
     * Returns {@code null} if either block is not dominated by the exit.
     */
    public BasicBlock lca(BasicBlock a, BasicBlock b) {
        Integer ia = indexOf.get(a);
        Integer ib = indexOf.get(b);
        if (ia == null || ib == null || depth[ia] < 0 || depth[ib] < 0) {
            return null;
        }
        int u = ia;
        int v = ib;
        if (depth[u] < depth[v]) {
            v = lift(v, depth[v] - depth[u]);
        } else if (depth[v] < depth[u]) {
            u = lift(u, depth[u] - depth[v]);
        }
        if (u == v) {
            return blocks.get(u);
        }
        for (int k = log - 1; k >= 0; k--) {
            int upU = up[u][k];
            int upV = up[v][k];
            if (upU != -1 && upV != -1 && upU != upV) {
                u = upU;
                v = upV;
            }
        }
        int ancestor = up[u][0];
        return ancestor == -1 ? null : blocks.get(ancestor);
    }

    /**
     * Returns true if {@code candidate} post-dominates {@code block}.
     */
    public boolean postDominates(BasicBlock candidate, BasicBlock block) {
        Integer cIdx = indexOf.get(candidate);
        Integer bIdx = indexOf.get(block);
        if (cIdx == null || bIdx == null || depth[cIdx] < 0 || depth[bIdx] < 0) {
            return false;
        }
        return tin[cIdx] <= tin[bIdx] && tout[bIdx] <= tout[cIdx];
    }

    private void build(CFGBuilder.CFGResult cfg) {
        int n = blocks.size();
        if (n == 0) {
            return;
        }
        List<List<Integer>> reversedSucc = new ArrayList<>(n);
        List<List<Integer>> reversedPred = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            reversedSucc.add(new ArrayList<>());
            reversedPred.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            BasicBlock block = blocks.get(i);
            for (BasicBlock pred : block.getPredecessors()) {
                Integer idx = indexOf.get(pred);
                if (idx != null) {
                    reversedSucc.get(i).add(idx);
                }
            }
            for (BasicBlock succ : block.getSuccessors()) {
                Integer idx = indexOf.get(succ);
                if (idx != null) {
                    reversedPred.get(i).add(idx);
                }
            }
        }

        int root = indexOf.getOrDefault(exitBlock, -1);
        if (root == -1) {
            return;
        }

        int[] semi = new int[n];
        int[] parent = new int[n];
        int[] vertex = new int[n + 1];
        int[] label = new int[n];
        int[] ancestor = new int[n];
        int[] idom = new int[n];
        List<List<Integer>> bucket = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            bucket.add(new ArrayList<>());
            parent[i] = -1;
            label[i] = i;
            ancestor[i] = -1;
            depth[i] = -1;
            tin[i] = -1;
            tout[i] = -1;
            for (int k = 0; k < log; k++) {
                up[i][k] = -1;
            }
        }

        DfsState dfsState = new DfsState(semi, vertex, label, ancestor, parent, reversedSucc);
        dfsState.run(root);
        int dfsTime = dfsState.time;

        for (int i = dfsTime; i >= 2; i--) {
            int w = vertex[i];
            for (int v : reversedPred.get(w)) {
                if (semi[v] == 0) {
                    continue;
                }
                int u = eval(v, semi, label, ancestor);
                if (semi[u] < semi[w]) {
                    semi[w] = semi[u];
                }
            }
            int semiVertex = vertex[semi[w]];
            bucket.get(semiVertex).add(w);
            link(parent[w], w, ancestor);
            List<Integer> parentBucket = bucket.get(parent[w]);
            for (int v : parentBucket) {
                int u = eval(v, semi, label, ancestor);
                idom[v] = semi[u] < semi[v] ? u : parent[w];
            }
            parentBucket.clear();
        }

        for (int i = 2; i <= dfsTime; i++) {
            int w = vertex[i];
            if (idom[w] != vertex[semi[w]]) {
                idom[w] = idom[idom[w]];
            }
        }
        idom[root] = -1;

        for (int i = 0; i < n; i++) {
            BasicBlock block = blocks.get(i);
            if (semi[i] == 0) {
                immediatePostDominators.put(block, null);
                continue;
            }
            int parentIdx = idom[i];
            BasicBlock parentBlock = parentIdx >= 0 ? blocks.get(parentIdx) : null;
            immediatePostDominators.put(block, parentBlock);
            if (parentIdx >= 0) {
                treeChildren.get(parentIdx).add(i);
            }
        }

        if (dfsTime > 0) {
            depth[root] = 0;
            up[root][0] = root;
            for (int k = 1; k < log; k++) {
                up[root][k] = root;
            }
            timer = 0;
            dfsTree(root);
        }
    }

    private void dfsTree(int node) {
        tin[node] = timer++;
        for (int child : treeChildren.get(node)) {
            depth[child] = depth[node] + 1;
            up[child][0] = node;
            for (int k = 1; k < log; k++) {
                int ancestor = up[child][k - 1];
                up[child][k] = ancestor == -1 ? -1 : up[ancestor][k - 1];
            }
            dfsTree(child);
        }
        tout[node] = timer++;
    }

    private int lift(int node, int delta) {
        int v = node;
        int k = 0;
        while (delta > 0 && v != -1) {
            if ((delta & 1) == 1) {
                if (k >= log) {
                    return -1;
                }
                v = up[v][k];
            }
            delta >>= 1;
            k++;
        }
        return v;
    }

    private void link(int v, int w, int[] ancestor) {
        ancestor[w] = v;
    }

    private int eval(int v, int[] semi, int[] label, int[] ancestor) {
        if (ancestor[v] == -1) {
            return label[v];
        }
        compress(v, semi, label, ancestor);
        return label[v];
    }

    private void compress(int v, int[] semi, int[] label, int[] ancestor) {
        if (ancestor[ancestor[v]] != -1) {
            compress(ancestor[v], semi, label, ancestor);
            if (semi[label[ancestor[v]]] < semi[label[v]]) {
                label[v] = label[ancestor[v]];
            }
            ancestor[v] = ancestor[ancestor[v]];
        }
    }

    private static final class DfsState {
        private final int[] semi;
        private final int[] vertex;
        private final int[] label;
        private final int[] ancestor;
        private final int[] parent;
        private final List<List<Integer>> succ;
        private int time = 0;

        private DfsState(int[] semi,
                         int[] vertex,
                         int[] label,
                         int[] ancestor,
                         int[] parent,
                         List<List<Integer>> succ) {
            this.semi = semi;
            this.vertex = vertex;
            this.label = label;
            this.ancestor = ancestor;
            this.parent = parent;
            this.succ = succ;
        }

        private void run(int root) {
            dfs(root);
        }

        private void dfs(int v) {
            time++;
            semi[v] = time;
            vertex[time] = v;
            label[v] = v;
            ancestor[v] = -1;
            for (int w : succ.get(v)) {
                if (semi[w] != 0) {
                    continue;
                }
                parent[w] = v;
                dfs(w);
            }
        }
    }
}

