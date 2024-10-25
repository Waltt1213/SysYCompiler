package frontend.ast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Exp implements AstNode {
    private final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public boolean isArray() {
        return addExp.isArray();
    }

    public String getIdentName() {
        return addExp.getIdentName();
    }

    public int getLineno() {
        return addExp.getLineno();
    }

    public AddExp getAddExp() {
        return addExp;
    }

    @Override
    public String getSymbol() {
        return "<Exp>";
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
