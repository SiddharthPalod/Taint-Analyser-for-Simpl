package ast;

public class Id extends Expr {
    public String name;
    public Id(String name) { this.name = name; }
    public String toString(String indent) { return name; }
}

