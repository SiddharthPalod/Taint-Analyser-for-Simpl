import parser.*;
import ast.*;
import cfg.*;
import analysis.*;
import java.io.*;
import java.util.*;
import java_cup.runtime.*;

/**
 * Main entry point for Taint Analysis
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java Main <sourcefile> [--optimized|--brute]");
            System.exit(1);
        }
        
        String mode = args.length > 1 ? args[1] : "--basic";
        boolean optimized = "--optimized".equals(mode);
        boolean bruteForce = "--brute".equals(mode);
        if (!(optimized || bruteForce || "--basic".equals(mode))) {
            System.err.println("Unknown option: " + mode);
            System.err.println("Usage: java Main <sourcefile> [--optimized|--brute]");
            System.exit(1);
        }
        
        // Read input file
        String input = readFile(args[0]);
        System.out.println("=== INPUT PROGRAM ===");
        System.out.println(input);
        
        // Parse using JFlex/CUP
        S program;
        try {
            program = parser.TaintParserWrapper.parse(input);
            System.out.println("\n=== PARSED AST ===");
            System.out.println(program);
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }
        
        // Collect all variables
        Set<String> allVars = collectVariables(program);
        System.out.println("\n=== VARIABLES ===");
        System.out.println(allVars);
        
        // Build CFG
        CFGBuilder cfgBuilder = new CFGBuilder();
        CFGBuilder.CFGResult cfg = cfgBuilder.build(program);
        System.out.println("\n=== CONTROL FLOW GRAPH ===");
        for (BasicBlock block : cfg.allBlocks) {
            System.out.println(block);
            System.out.print("  Successors: ");
            for (BasicBlock succ : block.getSuccessors()) {
                System.out.print("BB" + succ.id + " ");
            }
            System.out.println();
        }
        
        // Generate DOT file for visualization
        try {
            CFGDotGenerator.writeDotFile(cfg, "cfg.dot");
            System.out.println("\n=== CFG DOT FILE GENERATED ===");
            System.out.println("Visualization saved to: cfg.dot");
            System.out.println("Generate PNG with: dot -Tpng cfg.dot -o cfg.png");
        } catch (IOException e) {
            System.err.println("Warning: Could not write cfg.dot file: " + e.getMessage());
        }
        
        // Run analysis
        TaintAnalyzer analyzer;
        if (optimized) {
            System.out.println("\n=== RUNNING OPTIMIZED ANALYSIS (with Post-Dominators) ===");
            analyzer = new OptimizedTaintAnalyzer(cfg, allVars);
        } else if (bruteForce) {
            System.out.println("\n=== RUNNING BRUTE-FORCE ANALYSIS ===");
            analyzer = new BruteForceTaintAnalyzer(cfg, allVars);
        } else {
            System.out.println("\n=== RUNNING BASIC ANALYSIS ===");
            analyzer = new TaintAnalyzer(cfg, allVars);
        }
        
        analyzer.analyze();
        
        // Report leaks
        System.out.println("\n=== LEAK REPORT ===");
        List<TaintAnalyzer.LeakReport> leaks = analyzer.reportLeaks();
        if (leaks.isEmpty()) {
            System.out.println("No leaks detected!");
        } else {
            for (TaintAnalyzer.LeakReport leak : leaks) {
                System.out.println(leak);
            }
        }
    }
    
    private static String readFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }
    
    private static Set<String> collectVariables(ASTNode node) {
        Set<String> vars = new HashSet<>();
        collectVariablesRec(node, vars);
        return vars;
    }
    
    private static void collectVariablesRec(ASTNode node, Set<String> vars) {
        if (node instanceof Id) {
            vars.add(((Id) node).name);
        } else if (node instanceof Assign) {
            Assign a = (Assign) node;
            vars.add(a.var);
            collectVariablesRec(a.expr, vars);
        } else if (node instanceof BinExpr) {
            BinExpr b = (BinExpr) node;
            collectVariablesRec(b.left, vars);
            collectVariablesRec(b.right, vars);
        } else if (node instanceof SinkExpr) {
            collectVariablesRec(((SinkExpr) node).expr, vars);
        } else if (node instanceof If) {
            If i = (If) node;
            collectVariablesRec(i.cond, vars);
            collectVariablesRec(i.thenBranch, vars);
            collectVariablesRec(i.elseBranch, vars);
        } else if (node instanceof While) {
            While w = (While) node;
            collectVariablesRec(w.cond, vars);
            collectVariablesRec(w.body, vars);
        } else if (node instanceof Seq) {
            for (Stmt s : ((Seq) node).stmts) {
                collectVariablesRec(s, vars);
            }
        } else if (node instanceof S) {
            collectVariablesRec(((S) node).body, vars);
        }
    }
}

