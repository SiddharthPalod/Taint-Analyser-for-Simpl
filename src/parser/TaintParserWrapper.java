package parser;

import ast.*;
import java.io.*;
import java_cup.runtime.*;

/**
 * Wrapper class that uses JFlex/CUP generated parser
 */
public class TaintParserWrapper {
    
    public static S parse(String input) throws Exception {
        // Create lexer
        TaintLexer lexer = new TaintLexer(new StringReader(input));
        
        // Create parser
        TaintParser parser = new TaintParser(lexer);
        
        // Parse
        parser.parse();
        
        // Get AST
        return parser.getAST();
    }
}

