package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LOrExp implements AstNode {
    private final ArrayList<LAndExp> lAndExps;

    public LOrExp(ArrayList<LAndExp> lAndExps) {
        this.lAndExps = lAndExps;
    }

    public ArrayList<LAndExp> getLAndExps() {
        return lAndExps;
    }

    @Override
    public String getSymbol() {
        return "<LOrExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(lAndExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < lAndExps.size(); i++) {
            lAndExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < lAndExps.size() - 1) {
                bw.write(TokenType.OR + " ||\n");
            }
        }
    }
}
