package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class InitVal implements AstNode {
    private final Exp exp;
    private final ArrayList<Exp> exps;
    private final Token stringConst;

    public InitVal(Exp exp) {
        this.exp = exp;
        exps = null;
        stringConst = null;
    }

    public InitVal(ArrayList<Exp> exps) {
        exp = null;
        this.exps = exps;
        stringConst = null;
    }

    public InitVal(Token stringConst) {
        exp = null;
        exps = null;
        this.stringConst = stringConst;
    }

    public boolean isExp() {
        return exp != null;
    }

    public boolean isExps() {
        return exps != null;
    }

    public boolean isStringConst() {
        return stringConst != null;
    }

    @Override
    public String getSymbol() {
        return "<InitVal>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        if (isExp()) {
            astNodes.add(exp);
        } else if (isExps()) {
            astNodes.addAll(exps);
        }
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        if (exp != null) {
            exp.printToFile(bw);
        }
        if (exps != null) {
            bw.write(TokenType.LBRACE + " {\n");
            if (!exps.isEmpty()) {
                exps.get(0).printToFile(bw);
                for (int i = 1; i < exps.size(); i++) {
                    bw.write(TokenType.COMMA + " ,\n");
                    exps.get(i).printToFile(bw);
                }
            }
            bw.write(TokenType.RBRACE + " }\n");
        }
        if (stringConst != null) {
            bw.write(stringConst + "\n");
        }
        bw.write(getSymbol() + "\n");
    }
}
