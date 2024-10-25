package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LAndExp implements AstNode {
    private final ArrayList<EqExp> eqExps;

    public LAndExp(ArrayList<EqExp> eqExps) {
        this.eqExps = eqExps;
    }

    public ArrayList<EqExp> getEqExps() {
        return eqExps;
    }

    @Override
    public String getSymbol() {
        return "<LAndExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(eqExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < eqExps.size(); i++) {
            eqExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < eqExps.size() - 1) {
                bw.write(TokenType.AND + " &&\n");
            }
        }
    }
}
