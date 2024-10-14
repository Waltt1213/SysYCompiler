package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainFuncDef implements AstNode {
    private final Block block;
    private final int lineno;

    public MainFuncDef(Block block, int lineno) {
        this.block = block;
        this.lineno = lineno;
    }

    public int getLineno() {
        return lineno;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String getSymbol() {
        return "<MainFuncDef>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(block);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(TokenType.INTTK + " int\n");
        bw.write(TokenType.MAINTK + " main\n");
        bw.write(TokenType.LPARENT + " (\n");
        bw.write(TokenType.RPARENT + " )\n");
        block.printToFile(bw);
        bw.write(getSymbol() + "\n");
    }
}
