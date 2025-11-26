#!/bin/bash
echo "========================================"
echo "Cleaning generated and compiled files"
echo "========================================"
echo

# Remove generated parser/lexer files
if [ -f "src/parser/TaintLexer.java" ]; then
    echo "Removing TaintLexer.java..."
    rm -f src/parser/TaintLexer.java
fi

if [ -f "src/parser/TaintParser.java" ]; then
    echo "Removing TaintParser.java..."
    rm -f src/parser/TaintParser.java
fi

if [ -f "src/parser/sym.java" ]; then
    echo "Removing sym.java..."
    rm -f src/parser/sym.java
fi

# Remove backup files
if [ -f "src/parser/TaintLexer.java~" ]; then
    echo "Removing backup file TaintLexer.java~..."
    rm -f src/parser/TaintLexer.java~
fi

# Remove compiled class files
if [ -d "bin" ]; then
    echo "Removing compiled .class files from bin/..."
    rm -rf bin
    echo "bin/ directory removed."
fi

# Remove generated dot file
if [ -f "cfg.dot" ]; then
    echo "Removing cfg.dot..."
    rm -f cfg.dot
fi

echo
echo "========================================"
echo "Clean completed!"
echo "========================================"
echo
echo "Removed files:"
echo "  - TaintLexer.java"
echo "  - TaintParser.java"
echo "  - sym.java"
echo "  - All .class files in bin/"
echo "  - cfg.dot (if present)"
echo
echo "To regenerate, run: ./build.sh"
echo

