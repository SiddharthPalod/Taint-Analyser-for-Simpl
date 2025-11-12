package ast;

public class Assign extends Stmt {
    public String var;
    public Expr expr;
    public Assign(String var, Expr expr) {
        this.var = var;
        this.expr = expr;
    }
    public String toString(String indent) {
        return indent + var + " = " + expr.toString("") + "\n";
    }
}

