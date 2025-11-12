#!/bin/bash

echo "Building Taint Analysis Tool..."

# Set paths to JFlex and CUP (update these to your installation paths)
JFLEX_JAR=${JFLEX_JAR:-"/usr/local/jflex/lib/jflex-full-1.9.1.jar"}
CUP_JAR=${CUP_JAR:-"/usr/local/java-cup/java-cup-11b.jar"}

# Check if JFlex and CUP are available
if [ ! -f "$JFLEX_JAR" ]; then
    echo "WARNING: JFlex jar not found at $JFLEX_JAR"
    echo "Please set JFLEX_JAR environment variable or update build.sh"
    echo "Continuing with existing generated files..."
fi

if [ ! -f "$CUP_JAR" ]; then
    echo "WARNING: CUP jar not found at $CUP_JAR"
    echo "Please set CUP_JAR environment variable or update build.sh"
    echo "Continuing with existing generated files..."
fi

# Generate lexer
if [ -f "$JFLEX_JAR" ]; then
    echo "Generating lexer from taint.flex..."
    java -jar "$JFLEX_JAR" -d src/parser src/parser/taint.flex
fi

# Generate parser
if [ -f "$CUP_JAR" ]; then
    echo "Generating parser from taint.cup..."
    java -jar "$CUP_JAR" -parser TaintParser -symbols sym -destdir src/parser src/parser/taint.cup
fi

# Create bin directory
mkdir -p bin

# Compile
echo "Compiling source files..."
javac -d bin -sourcepath src -cp "$CUP_JAR" src/Main.java src/ast/*.java src/cfg/*.java src/parser/*.java src/analysis/*.java

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Run with: java -cp \"bin:$CUP_JAR\" Main test/test1_explicit_flow.txt"
else
    echo "Build failed!"
    exit 1
fi
