package ast;

public class S extends ASTNode {
    public Stmt body;
    public S(Stmt body) { this.body = body; }
    public String toString(String indent) {
        return indent + "begin\n" +
               body.toString(indent + "  ") +
               indent + "end\n";
    }
}

