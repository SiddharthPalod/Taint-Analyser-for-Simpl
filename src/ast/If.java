package ast;

public class If extends Stmt {
    public Expr cond;
    public Stmt thenBranch, elseBranch;
    public If(Expr cond, Stmt thenBranch, Stmt elseBranch) {
        this.cond = cond;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("if ").append(cond.toString("")).append(" then\n");
        sb.append(thenBranch.toString(indent + "  "));
        sb.append(indent).append("else\n");
        sb.append(elseBranch.toString(indent + "  "));
        sb.append(indent).append("fi\n");
        return sb.toString();
    }
}