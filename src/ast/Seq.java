package ast;

import java.util.*;

public class Seq extends Stmt {
    public List<Stmt> stmts = new ArrayList<>();
    public void add(Stmt s) { stmts.add(s); }
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        for (Stmt s : stmts) sb.append(s.toString(indent));
        return sb.toString();
    }
}

