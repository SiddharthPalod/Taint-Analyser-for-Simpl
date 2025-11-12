package cfg;

import ast.*;
import java.util.*;

/**
 * Builds BasicBlock-based CFG from AST
 * Includes statement normalization (three-address code)
 */
public class CFGBuilder {
    private int tempCounter = 0;
    
    public static class CFGResult {
        public final BasicBlock entry;
        public final BasicBlock exit;
        public final List<BasicBlock> allBlocks;
        
        public CFGResult(BasicBlock entry, BasicBlock exit, List<BasicBlock> allBlocks) {
            this.entry = entry;
            this.exit = exit;
            this.allBlocks = allBlocks;
        }
    }
    
    public CFGResult build(ASTNode root) {
        if (!(root instanceof S)) {
            throw new IllegalArgumentException("Root must be Program (S)");
        }
        
        S s = (S) root;
        List<BasicBlock> allBlocks = new ArrayList<>();
        BuildResult br = buildStmt(s.body, allBlocks);
        
        return new CFGResult(br.start, br.end, allBlocks);
    }
    
    private static class BuildResult {
        BasicBlock start, end;
        BuildResult(BasicBlock start, BasicBlock end) {
            this.start = start;
            this.end = end;
        }
    }
    
    private BuildResult buildStmt(Stmt stmt, List<BasicBlock> allBlocks) {
        if (stmt instanceof Assign) {
            Assign a = (Assign) stmt;
            BasicBlock block = new BasicBlock();
            allBlocks.add(block);
            
            // Normalize the assignment expression
            List<BasicBlock.Statement> normalized = normalizeAssignment(a.var, a.expr, a.lineNumber);
            for (BasicBlock.Statement s : normalized) {
                block.addStatement(s);
            }
            
            return new BuildResult(block, block);
            
        } else if (stmt instanceof Seq) {
            Seq seq = (Seq) stmt;
            BasicBlock first = null, last = null;
            
            for (Stmt s : seq.stmts) {
                BuildResult br = buildStmt(s, allBlocks);
                if (first == null) first = br.start;
                if (last != null) last.addSuccessor(br.start);
                last = br.end;
            }
            
            if (first == null) {
                BasicBlock empty = new BasicBlock();
                allBlocks.add(empty);
                return new BuildResult(empty, empty);
            }
            return new BuildResult(first, last);
            
        } else if (stmt instanceof If) {
            If ifs = (If) stmt;
            BasicBlock condBlock = new BasicBlock();
            allBlocks.add(condBlock);
            
            // Add condition to block
            condBlock.addStatement(new BasicBlock.Statement((Object)ifs.cond, ifs.cond.lineNumber));
            
            BuildResult thenBR = buildStmt(ifs.thenBranch, allBlocks);
            BuildResult elseBR = buildStmt(ifs.elseBranch, allBlocks);
            
            condBlock.addSuccessor(thenBR.start);
            condBlock.addSuccessor(elseBR.start);
            
            // Merge block
            BasicBlock mergeBlock = new BasicBlock();
            allBlocks.add(mergeBlock);
            thenBR.end.addSuccessor(mergeBlock);
            elseBR.end.addSuccessor(mergeBlock);
            
            return new BuildResult(condBlock, mergeBlock);
            
        } else if (stmt instanceof While) {
            While wh = (While) stmt;
            BasicBlock condBlock = new BasicBlock();
            allBlocks.add(condBlock);
            
            // Add condition to block
            condBlock.addStatement(new BasicBlock.Statement((Object)wh.cond, wh.cond.lineNumber));
            
            BuildResult bodyBR = buildStmt(wh.body, allBlocks);
            condBlock.addSuccessor(bodyBR.start); // true branch
            bodyBR.end.addSuccessor(condBlock);  // loop back
            
            // Exit block
            BasicBlock exitBlock = new BasicBlock();
            allBlocks.add(exitBlock);
            condBlock.addSuccessor(exitBlock); // false branch
            
            return new BuildResult(condBlock, exitBlock);
        }
        
        throw new IllegalArgumentException("Unknown statement type: " + stmt.getClass());
    }
    
    /**
     * Normalize an assignment expression into three-address code
     * Returns a list of statements
     */
    private List<BasicBlock.Statement> normalizeAssignment(String targetVar, Expr expr, int lineNumber) {
        List<BasicBlock.Statement> result = new ArrayList<>();
        
        if (expr instanceof InputExpr) {
            // x = inputExpr()
            result.add(new BasicBlock.Statement(targetVar, lineNumber));
        } else if (expr instanceof Id) {
            // x = y
            result.add(new BasicBlock.Statement(targetVar, ((Id) expr).name, lineNumber));
        } else if (expr instanceof Num) {
            // x = constant (not tainted, but we still need the statement)
            String constVar = "const_" + ((Num) expr).value;
            result.add(new BasicBlock.Statement(targetVar, constVar, lineNumber));
        } else if (expr instanceof SinkExpr) {
            // sinkExpr(x) - extract the variable
            SinkExpr sinkExpr = (SinkExpr) expr;
            // Use line number from SinkExpr if available, otherwise use the assignment's line number
            int sinkLineNumber = (sinkExpr.lineNumber != -1) ? sinkExpr.lineNumber : lineNumber;
            Expr sinkArg = sinkExpr.expr;
            if (sinkArg instanceof Id) {
                result.add(new BasicBlock.Statement(((Id) sinkArg).name, sinkLineNumber, true));
            } else {
                // Complex sink expression - normalize first
                String tempVar = newTemp();
                result.addAll(normalizeAssignment(tempVar, sinkArg, sinkLineNumber));
                result.add(new BasicBlock.Statement(tempVar, sinkLineNumber, true));
            }
        } else if (expr instanceof BinExpr) {
            // x = y op z
            BinExpr bin = (BinExpr) expr;
            String leftVar = extractVar(bin.left);
            String rightVar = extractVar(bin.right);
            
            // If operands are complex, normalize them first
            if (!(bin.left instanceof Id || bin.left instanceof Num)) {
                String tempLeft = newTemp();
                result.addAll(normalizeAssignment(tempLeft, bin.left, lineNumber));
                leftVar = tempLeft;
            }
            if (!(bin.right instanceof Id || bin.right instanceof Num)) {
                String tempRight = newTemp();
                result.addAll(normalizeAssignment(tempRight, bin.right, lineNumber));
                rightVar = tempRight;
            }
            
            result.add(new BasicBlock.Statement(targetVar, leftVar, bin.op, rightVar, lineNumber));
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + expr.getClass());
        }
        
        return result;
    }
    
    private String extractVar(Expr expr) {
        if (expr instanceof Id) {
            return ((Id) expr).name;
        } else if (expr instanceof Num) {
            return "const_" + ((Num) expr).value;
        } else {
            // Complex expression - will be normalized separately
            return null;
        }
    }
    
    private String newTemp() {
        return "t" + (tempCounter++);
    }
}

