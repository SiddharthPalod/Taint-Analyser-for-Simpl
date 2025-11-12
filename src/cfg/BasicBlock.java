package cfg;

import java.util.*;

/**
 * Basic Block representation for CFG
 * Each block contains a list of normalized statements (three-address code)
 */
public class BasicBlock {
    private static int nextId = 0;
    public final int id;
    public final List<BasicBlock.Statement> statements;
    private final List<BasicBlock> successors;
    private final List<BasicBlock> predecessors;
    
    public BasicBlock() {
        this.id = nextId++;
        this.statements = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
    }
    
    public void addStatement(BasicBlock.Statement stmt) {
        statements.add(stmt);
    }
    
    public void addSuccessor(BasicBlock block) {
        if (block != null && !successors.contains(block)) {
            successors.add(block);
            block.predecessors.add(this);
        }
    }
    
    public List<BasicBlock> getSuccessors() {
        return new ArrayList<>(successors);
    }
    
    public List<BasicBlock> getPredecessors() {
        return new ArrayList<>(predecessors);
    }
    
    public boolean isEmpty() {
        return statements.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BB").append(id).append(":\n");
        for (Statement stmt : statements) {
            sb.append("  ").append(stmt).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock that = (BasicBlock) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
    
    /**
     * Normalized statement representation (three-address code)
     */
    public static class Statement {
    public enum Type {
        ASSIGN,      // x = y
        ASSIGN_BIN,  // x = y op z
        ASSIGN_INPUT, // x = inputExpr()
        SINK,        // sinkExpr(x)
        COND         // if/while condition (for control flow)
    }
    
    public final Type type;
    public final String var;      // for ASSIGN, ASSIGN_BIN, ASSIGN_INPUT
    public final String leftVar;  // for ASSIGN_BIN
    public final String rightVar; // for ASSIGN_BIN or ASSIGN
    public final String op;       // for ASSIGN_BIN
    public final Object condition; // for COND (ast.Expr)
    public final int lineNumber;
    
    // Simple assignment: x = y
    public Statement(String var, String rightVar, int lineNumber) {
        this.type = Type.ASSIGN;
        this.var = var;
        this.rightVar = rightVar;
        this.leftVar = null;
        this.op = null;
        this.condition = null;
        this.lineNumber = lineNumber;
    }
    
    // Binary assignment: x = y op z
    public Statement(String var, String leftVar, String op, String rightVar, int lineNumber) {
        this.type = Type.ASSIGN_BIN;
        this.var = var;
        this.leftVar = leftVar;
        this.rightVar = rightVar;
        this.op = op;
        this.condition = null;
        this.lineNumber = lineNumber;
    }
    
    // Input assignment: x = inputExpr()
    public Statement(String var, int lineNumber) {
        this.type = Type.ASSIGN_INPUT;
        this.var = var;
        this.rightVar = null;
        this.leftVar = null;
        this.op = null;
        this.condition = null;
        this.lineNumber = lineNumber;
    }
    
    // Sink: sinkExpr(x)
    public Statement(String var, int lineNumber, boolean isSink) {
        if (!isSink) throw new IllegalArgumentException("Use other constructors for non-sink statements");
        this.type = Type.SINK;
        this.var = var;
        this.rightVar = null;
        this.leftVar = null;
        this.op = null;
        this.condition = null;
        this.lineNumber = lineNumber;
    }
    
    // Condition: for control flow
    public Statement(Object condition, int lineNumber) {
        this.type = Type.COND;
        this.var = null;
        this.rightVar = null;
        this.leftVar = null;
        this.op = null;
        this.condition = condition;
        this.lineNumber = lineNumber;
    }
    
    @Override
    public String toString() {
        switch (type) {
            case ASSIGN:
                return var + " = " + rightVar;
            case ASSIGN_BIN:
                return var + " = " + leftVar + " " + op + " " + rightVar;
            case ASSIGN_INPUT:
                return var + " = inputExpr()";
            case SINK:
                return "sinkExpr(" + var + ")";
            case COND:
                return "cond: " + (condition != null ? condition.toString() : "null");
            default:
                return "UNKNOWN";
        }
    }
    }
}

