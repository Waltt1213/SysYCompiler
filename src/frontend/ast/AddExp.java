package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AddExp implements AstNode {
    private final ArrayList<MulExp> mulExps;
    private final ArrayList<Token> ops;

    public AddExp(ArrayList<MulExp> mulExps, ArrayList<Token> ops) {
        this.mulExps = mulExps;
        this.ops = ops;
    }

    public boolean isArray() {
        if (mulExps.size() == 1) {
            return mulExps.get(0).isArray();
        }
        return false;
    }

    public int getLineno() {
        return mulExps.get(0).getLineno();
    }

    public String getIdentName() {
        if (mulExps.size() == 1) {
            return mulExps.get(0).getIdentName();
        }
        return "";
    }

    public ArrayList<MulExp> getMulExps() {
        return mulExps;
    }

    public ArrayList<Token> getOps() {
        return ops;
    }

    @Override
    public String getSymbol() {
        return "<AddExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(mulExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < mulExps.size(); i++) {
            mulExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < ops.size()) {
                bw.write(ops.get(i) + "\n");
            }
        }
    }
}
