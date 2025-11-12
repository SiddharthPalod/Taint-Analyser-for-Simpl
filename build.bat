@echo off
echo Building Taint Analysis Tool...

REM Set paths to JFlex and CUP (update these to your installation paths)
if not defined JFLEX_JAR (
    echo JFLEX_JAR environment variable not set, using default path...
    set JFLEX_JAR=D:/Siddharth/jflex-1.9.1/lib/jflex-full-1.9.1.jar
)
if not defined CUP_JAR (
    echo CUP_JAR environment variable not set, using default path...
    set CUP_JAR=D:/Siddharth/java-cup-bin-11b-20160615/java-cup-11b.jar
)

REM Check if JFlex and CUP are available
if not exist "%JFLEX_JAR%" (
    echo ERROR: JFlex jar not found at %JFLEX_JAR%
    echo Please set JFLEX_JAR environment variable or update build.bat
    echo Download JFlex from https://www.jflex.de/
    pause
    exit /b 1
)

if not exist "%CUP_JAR%" (
    echo ERROR: CUP jar not found at %CUP_JAR%
    echo Please set CUP_JAR environment variable or update build.bat
    echo Download Java CUP from http://www2.cs.tum.edu/projects/cup/
    pause
    exit /b 1
)

echo Step 1: Generating lexer from taint.flex...
java -jar "%JFLEX_JAR%" -d src\parser src\parser\taint.flex
if %errorlevel% neq 0 (
    echo ERROR: JFlex generation failed!
    pause
    exit /b %errorlevel%
)

REM Add package declaration and imports to generated TaintLexer.java
echo Adding package and imports to TaintLexer.java...
powershell -Command "$content = Get-Content src\parser\TaintLexer.java -Raw; $newContent = 'package parser;' + [Environment]::NewLine + [Environment]::NewLine + 'import java_cup.runtime.Symbol;' + [Environment]::NewLine + 'import ast.*;' + [Environment]::NewLine + [Environment]::NewLine + $content; Set-Content src\parser\TaintLexer.java -Value $newContent -NoNewline"

echo Step 2: Generating parser from taint.cup...
java -jar "%CUP_JAR%" -parser TaintParser -symbols sym -destdir src\parser src\parser\taint.cup
if %errorlevel% neq 0 (
    echo ERROR: CUP generation failed!
    pause
    exit /b %errorlevel%
)

if not exist bin mkdir bin

echo Compiling source files...
javac -d bin -sourcepath src -cp "%CUP_JAR%" src\Main.java src\ast\*.java src\cfg\*.java src\parser\*.java src\analysis\*.java

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo Run with: java -cp "bin;%CUP_JAR%" Main test\test1_explicit_flow.txt
) else (
    echo Build failed!
    exit /b 1
)

