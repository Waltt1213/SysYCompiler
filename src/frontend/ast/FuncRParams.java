package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FuncRParams implements AstNode {
    private final ArrayList<Exp> exps;
    private final int argc;

    public FuncRParams(ArrayList<Exp> exps) {
        this.exps = exps;
        argc = exps.size();
    }

    public Exp getParam(int i) {
        return exps.get(i);
    }

    public int getArgc() {
        return argc;
    }

    @Override
    public String getSymbol() {
        return "<FuncRParams>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(exps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        exps.get(0).printToFile(bw);
        for (int i = 1; i < exps.size(); i++) {
            bw.write(TokenType.COMMA + " ,\n");
            exps.get(i).printToFile(bw);
        }
        bw.write(getSymbol() + "\n");
    }
}
