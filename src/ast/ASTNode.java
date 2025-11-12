package ast;

/**
 * Base class for AST nodes
 */
public abstract class ASTNode {
    public int lineNumber = -1;
    public abstract String toString(String indent);
    @Override
    public String toString() { return toString(""); }
}

