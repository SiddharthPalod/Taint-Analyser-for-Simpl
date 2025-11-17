import analysis.BruteForceTaintAnalyzer;
import analysis.OptimizedTaintAnalyzer;
import analysis.TaintAnalyzer;
import ast.*;
import cfg.CFGBuilder;
import parser.TaintParserWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility to benchmark the runtime of the basic, brute-force, and optimized
 * taint analyzers across a suite of test inputs. Results are printed to the
 * terminal and persisted to a CSV file for further inspection.
 *
 * Usage:
 *   java -cp "bin;CUP_JAR" BenchmarkRunner [testsDir] [outputCsv]
 *
 * Defaults:
 *   testsDir  -> "test"
 *   outputCsv -> "benchmark_results.csv"
 */
public class BenchmarkRunner {
    private static final String DEFAULT_TEST_DIR = "test";
    private static final String DEFAULT_OUTPUT_CSV = "benchmark_results.csv";

    public static void main(String[] args) throws Exception {
        String testsDir = args.length > 0 ? args[0] : DEFAULT_TEST_DIR;
        String outputCsv = args.length > 1 ? args[1] : DEFAULT_OUTPUT_CSV;

        File dir = new File(testsDir);
        if (!dir.isDirectory()) {
            System.err.println("Test directory not found: " + dir.getAbsolutePath());
            System.exit(1);
        }

        File[] testFiles = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (testFiles == null || testFiles.length == 0) {
            System.err.println("No .txt test files found in " + dir.getAbsolutePath());
            System.exit(1);
        }
        Arrays.sort(testFiles);

        System.out.println("Performing JVM warm-up using " + testFiles[0].getName());
        warmUpAnalyzers(prepareBenchmarkInput(testFiles[0]));

        List<BenchmarkResult> results = new ArrayList<>();
        for (File testFile : testFiles) {
            BenchmarkInput input = prepareBenchmarkInput(testFile);
            System.out.println("Benchmarking " + testFile.getName());

            results.add(runAnalyzer(testFile.getName(), "basic",
                    () -> new TaintAnalyzer(input.cfg, input.allVars)));
            results.add(runAnalyzer(testFile.getName(), "brute",
                    () -> new BruteForceTaintAnalyzer(input.cfg, input.allVars)));
            results.add(runAnalyzer(testFile.getName(), "optimized",
                    () -> new OptimizedTaintAnalyzer(input.cfg, input.allVars)));
        }

        printResults(results);
        writeCsv(results, outputCsv);
        System.out.println("Benchmark results saved to " + outputCsv);
    }

    private static BenchmarkInput prepareBenchmarkInput(File testFile) throws Exception {
        String source = readFile(testFile);
        S program = TaintParserWrapper.parse(source);
        Set<String> vars = collectVariables(program);
        CFGBuilder.CFGResult cfg = new CFGBuilder().build(program);
        return new BenchmarkInput(testFile.getName(), cfg, vars);
    }

    private static BenchmarkResult runAnalyzer(String testName, String mode,
                                               Supplier<TaintAnalyzer> analyzerSupplier) {
        TaintAnalyzer analyzer = analyzerSupplier.get();
        long start = System.nanoTime();
        analyzer.analyze();
        long durationNs = System.nanoTime() - start;
        int leakCount = analyzer.reportLeaks().size();
        double durationMs = durationNs / 1_000_000.0;
        return new BenchmarkResult(testName, mode, durationMs, leakCount);
    }

    private static void warmUpAnalyzers(BenchmarkInput input) {
        List<Supplier<TaintAnalyzer>> analyzers = Arrays.asList(
                () -> new TaintAnalyzer(input.cfg, input.allVars),
                () -> new BruteForceTaintAnalyzer(input.cfg, input.allVars),
                () -> new OptimizedTaintAnalyzer(input.cfg, input.allVars)
        );
        for (Supplier<TaintAnalyzer> supplier : analyzers) {
            TaintAnalyzer analyzer = supplier.get();
            analyzer.analyze();
            analyzer.reportLeaks();
        }
        System.out.println("Warm-up completed for " + input.testName);
    }

    private static void printResults(List<BenchmarkResult> results) {
        System.out.println("\n=== BENCHMARK RESULTS ===");
        System.out.printf("%-12s %-10s %12s %8s%n", "Test", "Mode", "Duration(ms)", "Leaks");
        for (BenchmarkResult result : results) {
            System.out.printf("%-12s %-10s %12.3f %8d%n",
                    result.testName, result.mode, result.durationMs, result.leakCount);
        }
    }

    private static void writeCsv(List<BenchmarkResult> results, String outputCsv) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsv))) {
            writer.println("test,mode,duration_ms,leaks");
            for (BenchmarkResult result : results) {
                writer.printf("%s,%s,%.3f,%d%n",
                        result.testName, result.mode, result.durationMs, result.leakCount);
            }
        }
    }

    private static String readFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
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

    private static class BenchmarkInput {
        final String testName;
        final CFGBuilder.CFGResult cfg;
        final Set<String> allVars;

        BenchmarkInput(String testName, CFGBuilder.CFGResult cfg, Set<String> allVars) {
            this.testName = testName;
            this.cfg = cfg;
            this.allVars = allVars;
        }
    }

    private static class BenchmarkResult {
        final String testName;
        final String mode;
        final double durationMs;
        final int leakCount;

        BenchmarkResult(String testName, String mode, double durationMs, int leakCount) {
            this.testName = testName;
            this.mode = mode;
            this.durationMs = durationMs;
            this.leakCount = leakCount;
        }
    }
}


