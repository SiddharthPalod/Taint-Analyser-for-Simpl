# Taint Analysis Tool

A static taint analysis tool implementing the Kildall worklist algorithm for data-flow analysis, with support for explicit and implicit flow detection.

## Features

- **Explicit Flow Detection**: Tracks taint from `inputExpr()` sources to `sinkExpr()` sinks
- **Implicit Flow Detection**: Detects taint propagation through control flow (if/while conditions)
- **Basic Block CFG**: Converts AST to Control Flow Graph with normalized statements (three-address code)
- **Kildall Worklist Algorithm**: Efficient fixed-point solver for data-flow analysis
- **Post-Dominator Optimization**: Optional optimization to reduce false positives by resetting control taint at merge points
- **Brute-Force Fixed Point**: Reference solver that iterates the entire CFG each round
- **JFlex/CUP Parser**: Uses JFlex for lexing and CUP for parsing (as specified in the architecture)

## Prerequisites

1. **Java JDK** (Java 8 or later)
2. **JFlex** - Download from https://www.jflex.de/
   - Extract and note the path to `jflex-full-*.jar`
3. **Java CUP** - Download from http://www2.cs.tum.edu/projects/cup/
   - Extract and note the path to `java-cup-11b.jar`

## Building

### Windows
```cmd
set JFLEX_JAR=C:\path\to\jflex-full-1.9.1.jar
set CUP_JAR=C:\path\to\java-cup-11b.jar
build.bat
```

### Linux/Mac
```bash
export JFLEX_JAR=/path/to/jflex-full-1.9.1.jar
export CUP_JAR=/path/to/java-cup-11b.jar
bash build.sh
```

The build script will:
1. Generate `TaintLexer.java` from `taint.flex` using JFlex
2. Generate `TaintParser.java` and `sym.java` from `taint.cup` using CUP
3. Compile all Java files

## Running

### Basic Analysis
```bash
java -cp "bin;CUP_JAR" Main test/test1_explicit_flow.txt
```

### Brute-Force Fixed-Point Analysis
```bash
java -cp "bin;CUP_JAR" Main test/test1_explicit_flow.txt --brute
```

### Optimized Analysis (with Post-Dominators)
```bash
java -cp "bin;CUP_JAR" Main test/test1_explicit_flow.txt --optimized
```

**Windows:**
```cmd
java -cp "bin;%CUP_JAR%" Main test\test1_explicit_flow.txt --brute
```

### Benchmarking All Modes
Run the benchmark harness to compare basic, brute-force, and optimized analyzers across every `.txt` file in a directory (defaults to `test`) and export the timings to a CSV file (defaults to `benchmark_results.csv`):

```bash
java -cp "bin;CUP_JAR" BenchmarkRunner [testsDir] [outputCsv]
```

Example (Windows):

```cmd
java -cp "bin;%CUP_JAR%" BenchmarkRunner test benchmark_results.csv
```

## Test Cases

1. **test1_explicit_flow.txt**: Basic explicit flow (should detect leak)
2. **test2_implicit_flow.txt**: Implicit flow through if statement (should detect leak)
3. **test3_false_positive.txt**: False positive case (optimized version should not detect leak)
4. **test4_loop_flow.txt**: Loop flow (should detect leak)
5. **test5_sanitized.txt**: Sanitized flow (should not detect leak)

## Language Syntax

```
program ::= begin stmt_list end
stmt ::= ID = expr ;
       | if expr then stmt_list else stmt_list fi
       | while expr do stmt_list done
stmt_list ::= stmt | stmt_list stmt
expr ::= inputExpr()
       | sinkExpr(expr)
       | ID
       | NUM
       | expr op expr
       | (expr)
op ::= + | - | * | / | % | == | != | < | <= | > | >=
```

## Architecture

### Phase 1: Language Specification and Front-End (JFlex/CUP)
- **Lexer**: JFlex-generated `TaintLexer.java` from `taint.flex`
- **Parser**: CUP-generated `TaintParser.java` from `taint.cup`
- **AST**: Java classes in `ast/` package

### Phase 2: AST to CFG Conversion
- **BasicBlock**: Represents basic blocks in the CFG
- **CFGBuilder**: Converts AST to CFG with statement normalization
- **Statement Normalization**: Converts complex expressions to three-address code

### Phase 3: Taint Analysis
- **AnalysisState**: Represents the analysis lattice (Map<Var, TaintState> × ControlTaint)
- **TaintAnalyzer**: Implements Kildall worklist algorithm
- **Transfer Functions**: Implements taint propagation rules including implicit flow

### Phase 4: Optimization
- **OptimizedTaintAnalyzer**: Extends basic analyzer with post-dominator tree optimization
- **Post-Dominator Computation**: Calculates post-dominators to reset control taint at merge points

## Algorithm Details

### Analysis State
- **Lattice**: V = M × C where:
  - M: Map from variables to taint states {NT, T}
  - C: Control taint {NT, T}
- **Bottom State**: (λv.NT, NT)
- **Join Operation**: Pointwise join of maps and control taint

### Transfer Functions
- `x = inputExpr()`: x → T
- `x = y`: x → join(y, C)
- `x = y op z`: x → join(y, z, C)
- `if (cond)`: C → join(C, taint(cond))
- `sinkExpr(x)`: Report leak if x is tainted

### Post-Dominator Optimization
When leaving an if/while structure, control taint is reset to the taint state of the immediate post-dominator, reducing false positives.

## Output

The tool outputs:
1. Input program
2. Parsed AST
3. Variables
4. Control Flow Graph
5. Leak Report (if any leaks detected)

Example leak report:
```
LEAK at line 3: variable 'x' is tainted at sinkExpr
```
