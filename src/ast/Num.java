package ast;

public class Num extends Expr {
    public int value;
    public Num(int value) { this.value = value; }
    public String toString(String indent) { return String.valueOf(value); }
}

