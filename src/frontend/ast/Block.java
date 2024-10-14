package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Block implements AstNode {
    private final ArrayList<AstNode> blockItems;
    private final boolean hasRet;
    private final int lineno;

    public Block(ArrayList<AstNode> blockItems, boolean hasRet, int lineno) {
        this.blockItems = blockItems;
        this.hasRet = hasRet;
        this.lineno = lineno;
    }

    public int getLineno() {
        return lineno;
    }

    public boolean hasRet() {
        return hasRet;
    }

    @Override
    public String getSymbol() {
        return "<Block>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(blockItems);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(TokenType.LBRACE + " {\n");
        for (AstNode blockItem : blockItems) {
            blockItem.printToFile(bw);
        }
        bw.write(TokenType.RBRACE + " }\n");
        bw.write(getSymbol() + "\n");
    }
}
