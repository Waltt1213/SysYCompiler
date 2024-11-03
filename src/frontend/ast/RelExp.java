package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class RelExp implements AstNode {
    private final ArrayList<AddExp> addExps;
    private final ArrayList<Token> ops;

    public RelExp(ArrayList<AddExp> addExps, ArrayList<Token> ops) {
        this.addExps = addExps;
        this.ops = ops;
    }

    public ArrayList<AddExp> getAddExps() {
        return addExps;
    }

    public ArrayList<Token> getOps() {
        return ops;
    }

    @Override
    public String getSymbol() {
        return "<RelExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(addExps);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (int i = 0; i < addExps.size(); i++) {
            addExps.get(i).printToFile(bw);
            bw.write(getSymbol() + "\n");
            if (i < ops.size()) {
                bw.write(ops.get(i) + "\n");
            }
        }
    }
}
