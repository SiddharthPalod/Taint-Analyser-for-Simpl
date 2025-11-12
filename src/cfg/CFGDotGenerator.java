package cfg;

import ast.*;
import java.util.*;
import java.io.*;

/**
 * Generates DOT file for visualizing block-based CFG
 */
public class CFGDotGenerator {
    
    /**
     * Generate DOT file content from CFG result
     */
    public static String generateDot(CFGBuilder.CFGResult cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG {\n");
        sb.append("    node [shape=box];\n");
        sb.append("    rankdir=TB;\n\n");
        
        // Generate nodes with labels
        for (BasicBlock block : cfg.allBlocks) {
            String label = formatBlockLabel(block);
            sb.append("    ").append(block.id).append(" [label=\"").append(escapeLabel(label)).append("\"];\n");
        }
        
        sb.append("\n");
        
        // Generate edges
        for (BasicBlock block : cfg.allBlocks) {
            for (BasicBlock succ : block.getSuccessors()) {
                sb.append("    ").append(block.id).append(" -> ").append(succ.id).append(";\n");
            }
        }
        
        sb.append("}\n");
        return sb.toString();
    }
    
    /**
     * Format a basic block's label showing all statements
     */
    private static String formatBlockLabel(BasicBlock block) {
        if (block.isEmpty()) {
            return "BB" + block.id + "\\n(empty)";
        }
        
        StringBuilder label = new StringBuilder("BB" + block.id);
        for (BasicBlock.Statement stmt : block.statements) {
            label.append("\\n").append(formatStatement(stmt));
        }
        return label.toString();
    }
    
    /**
     * Format a statement for display
     */
    private static String formatStatement(BasicBlock.Statement stmt) {
        switch (stmt.type) {
            case ASSIGN:
                return stmt.var + " = " + stmt.rightVar;
            case ASSIGN_BIN:
                return stmt.var + " = " + stmt.leftVar + " " + stmt.op + " " + stmt.rightVar;
            case ASSIGN_INPUT:
                return stmt.var + " = inputExpr()";
            case SINK:
                return "sinkExpr(" + stmt.var + ")";
            case COND:
                if (stmt.condition instanceof Expr) {
                    return "if " + formatCondition((Expr)stmt.condition);
                }
                return "cond: " + (stmt.condition != null ? stmt.condition.toString() : "null");
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Format a condition expression for display
     */
    private static String formatCondition(Expr expr) {
        if (expr instanceof Id) {
            return ((Id) expr).name;
        } else if (expr instanceof Num) {
            return String.valueOf(((Num) expr).value);
        } else if (expr instanceof BinExpr) {
            BinExpr bin = (BinExpr) expr;
            return "(" + formatCondition(bin.left) + " " + bin.op + " " + formatCondition(bin.right) + ")";
        } else if (expr instanceof InputExpr) {
            return "inputExpr()";
        } else {
            return expr.toString();
        }
    }
    
    /**
     * Escape special characters in DOT labels
     */
    private static String escapeLabel(String label) {
        // Replace backslashes first, then quotes
        return label.replace("\\", "\\\\")
                   .replace("\"", "\\\"");
    }
    
    /**
     * Write DOT file to disk
     */
    public static void writeDotFile(CFGBuilder.CFGResult cfg, String filename) throws IOException {
        String dotContent = generateDot(cfg);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(dotContent);
        }
    }
}

