package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MulExp implements AstNode {
    private final ArrayList<UnaryExp> unaryExps;
    private final ArrayList<Token> ops;

    public MulExp(ArrayList<UnaryExp> unaryExps, ArrayList<Token> ops) {
        this.unaryExps = unaryExps;
        this.ops = ops;
    }

    public boolean isArray() {
        if (unaryExps.size() == 1) {
            return unaryExps.get(0).isArray();
        }
        return false;
    }

    public String getIdentName() {
        if (unaryExps.size() == 1) {
            return unaryExps.get(0).getIdentName();
        }
        return "";
    }

    public ArrayList<UnaryExp> getUnaryExps() {
        return unaryExps;
    }

    public int getLineno() {
        return unaryExps.get(0).getLineno();
    }

    @Override
    public String getSymbol() {
        return "<MulExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(unaryExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < unaryExps.size(); i++) {
            unaryExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < ops.size()) {
                bw.write(ops.get(i) + "\n");
            }
        }
    }
}
