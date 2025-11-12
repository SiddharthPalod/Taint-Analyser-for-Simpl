package ast;

public class While extends Stmt {
    public Expr cond;
    public Stmt body;
    public While(Expr cond, Stmt body) {
        this.cond = cond;
        this.body = body;
    }
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("while ").append(cond.toString("")).append(" do\n");
        sb.append(body.toString(indent + "  "));
        sb.append(indent).append("done\n");
        return sb.toString();
    }
}

