package frontend.ast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ConstExp implements AstNode {
    private final AddExp addExp;

    public ConstExp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return addExp;
    }

    @Override
    public String getSymbol() {
        return "<ConstExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(addExp);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        addExp.printToFile(bw);
        bw.write(getSymbol() + "\n");
    }
}
