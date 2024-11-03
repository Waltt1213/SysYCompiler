package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class EqExp implements AstNode {
    private final ArrayList<RelExp> relExps;
    private final ArrayList<Token> ops;

    public EqExp(ArrayList<RelExp> relExps, ArrayList<Token> ops) {
        this.relExps = relExps;
        this.ops = ops;
    }

    public ArrayList<RelExp> getRelExps() {
        return relExps;
    }

    public ArrayList<Token> getOps() {
        return ops;
    }

    @Override
    public String getSymbol() {
        return "<EqExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(relExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < relExps.size(); i++) {
            relExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < ops.size()) {
                bw.write(ops.get(i) + "\n");
            }
        }
    }
}
