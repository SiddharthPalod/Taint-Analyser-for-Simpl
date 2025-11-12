@echo off
echo ========================================
echo Cleaning generated and compiled files
echo ========================================
echo.

REM Remove generated parser/lexer files
if exist src\parser\TaintLexer.java (
    echo Removing TaintLexer.java...
    del /Q src\parser\TaintLexer.java
)

if exist src\parser\TaintParser.java (
    echo Removing TaintParser.java...
    del /Q src\parser\TaintParser.java
)

if exist src\parser\sym.java (
    echo Removing sym.java...
    del /Q src\parser\sym.java
)

REM Remove backup files
if exist src\parser\TaintLexer.java~ (
    echo Removing backup file TaintLexer.java~...
    del /Q src\parser\TaintLexer.java~
)

REM Remove compiled class files
if exist bin (
    echo Removing compiled .class files from bin\...
    rmdir /S /Q bin
    echo bin\ directory removed.
)

REM Remove generated dot file
if exist cfg.dot (
    echo Removing cfg.dot...
    del /Q cfg.dot
)

echo.
echo ========================================
echo Clean completed!
echo ========================================
echo.
echo Removed files:
echo   - TaintLexer.java
echo   - TaintParser.java
echo   - sym.java
echo   - All .class files in bin\
echo   - cfg.dot (if present)
echo.
echo To regenerate, run: build.bat
echo.
pause

