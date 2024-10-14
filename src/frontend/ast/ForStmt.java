package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ForStmt implements AstNode {
    private final LVal lval;
    private final Exp exp;
    private final int lineno;

    public ForStmt(LVal lval, Exp exp) {
        this.lval = lval;
        this.exp = exp;
        this.lineno = exp.getLineno();
    }

    public int getLineno() {
        return lineno;
    }

    @Override
    public String getSymbol() {
        return "<ForStmt>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(lval);
        astNodes.add(exp);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        lval.printToFile(bw);
        bw.write(TokenType.ASSIGN + " =\n");
        exp.printToFile(bw);
        bw.write(getSymbol() + "\n");
    }
}
