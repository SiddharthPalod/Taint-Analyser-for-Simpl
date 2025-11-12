package ast;

public class SinkExpr extends Expr {
    public Expr expr;
    public SinkExpr(Expr expr) { this.expr = expr; }
    public String toString(String indent) { 
        return "sinkExpr(" + expr.toString("") + ")"; 
    }
}

