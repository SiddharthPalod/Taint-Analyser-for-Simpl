%%
%class TaintLexer
%unicode
%cup
%line
%column

%{
// Helper methods to create Symbol objects for CUP
private java_cup.runtime.Symbol symbol(int type) {
    return new java_cup.runtime.Symbol(type, yyline+1, yycolumn+1);
}

private java_cup.runtime.Symbol symbol(int type, Object value) {
    return new java_cup.runtime.Symbol(type, yyline+1, yycolumn+1, value);
}
%}

%%

"begin"|"BEGIN"     { return symbol(sym.BEGIN); }
"end"|"END"         { return symbol(sym.END); }
"if"|"IF"           { return symbol(sym.IF); }
"then"|"THEN"       { return symbol(sym.THEN); }
"else"|"ELSE"       { return symbol(sym.ELSE); }
"fi"|"FI"           { return symbol(sym.FI); }
"while"|"WHILE"     { return symbol(sym.WHILE); }
"do"|"DO"           { return symbol(sym.DO); }
"done"|"DONE"       { return symbol(sym.DONE); }
"inputExpr"         { return symbol(sym.INPUTEXPR); }
"sinkExpr"          { return symbol(sym.SINKEXPR); }

[0-9]+              { return symbol(sym.NUM, Integer.parseInt(yytext())); }
[a-zA-Z_][a-zA-Z0-9_]* { return symbol(sym.ID, yytext()); }

"="                 { return symbol(sym.ASSIGN); }
"=="                { return symbol(sym.EQ); }
"!="                { return symbol(sym.NE); }
"<="                { return symbol(sym.LE); }
">="                { return symbol(sym.GE); }
"<"                 { return symbol(sym.LT); }
">"                 { return symbol(sym.GT); }
"+"                 { return symbol(sym.PLUS); }
"-"                 { return symbol(sym.MINUS); }
"*"                 { return symbol(sym.TIMES); }
"/"                 { return symbol(sym.DIV); }
"%"                 { return symbol(sym.MOD); }
";"                 { return symbol(sym.SEMI); }
"("                 { return symbol(sym.LPAREN); }
")"                 { return symbol(sym.RPAREN); }

[ \t\r\n]+          { /* skip whitespace */ }
.                   { System.err.println("Illegal char: " + yytext() + " at line " + (yyline+1)); }

