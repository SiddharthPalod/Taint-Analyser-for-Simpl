#!/bin/bash

echo "Building Taint Analysis Tool..."

# Set paths to JFlex and CUP
# (Keep your existing paths that you exported in the terminal)
JFLEX_JAR=${JFLEX_JAR:-"/Users/pratheekp/IdeaProjects/Taint-Analyser-for-Simpl/jflex-full-1.9.1.jar"}
CUP_JAR=${CUP_JAR:-"/Users/pratheekp/IdeaProjects/Taint-Analyser-for-Simpl/java-cup-11b.jar"}

# Check if JFlex and CUP are available
if [ ! -f "$JFLEX_JAR" ]; then
    echo "WARNING: JFlex jar not found at $JFLEX_JAR"
fi

if [ ! -f "$CUP_JAR" ]; then
    echo "WARNING: CUP jar not found at $CUP_JAR"
fi

# 1. Generate lexer (JFlex)
if [ -f "$JFLEX_JAR" ]; then
    echo "Generating lexer from taint.flex..."
    java -jar "$JFLEX_JAR" -d src/parser src/parser/taint.flex
fi

# 2. Generate parser (CUP) - FIX: Use -cp instead of -jar
if [ -f "$CUP_JAR" ]; then
    echo "Generating parser from taint.cup..."
    # We use java_cup.Main explicitly to avoid "no main manifest" errors
    java -cp "$CUP_JAR" java_cup.Main -parser TaintParser -symbols sym -destdir src/parser src/parser/taint.cup
fi

# Create bin directory
mkdir -p bin

# 3. Compile
echo "Compiling source files..."
# Added -Xlint:unchecked for better visibility, though not strictly necessary
javac -d bin -sourcepath src -cp "$CUP_JAR" src/Main.java src/ast/*.java src/cfg/*.java src/parser/*.java src/analysis/*.java

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Run with: java -cp \"bin:$CUP_JAR\" Main test/test1_explicit_flow.txt"
else
    echo "Build failed!"
    exit 1
fi