package ast;

public class BinExpr extends Expr {
    public String op;
    public Expr left, right;
    public BinExpr(Expr left, String op, Expr right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
    public String toString(String indent) {
        return "(" + left.toString("") + " " + op + " " + right.toString("") + ")";
    }
}

