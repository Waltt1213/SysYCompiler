package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FuncDef implements AstNode {
    private final FuncType funcType;
    private final Token ident;
    private final FuncFParams funcFParams;
    private final Block block;
    private final int argc;

    public FuncDef(FuncType type, Token ident, FuncFParams funcFParams, Block block) {
        funcType = type;
        this.ident = ident;
        this.funcFParams = funcFParams;
        this.block = block;
        argc = funcFParams.getArgc();
    }

    public FuncDef(FuncType type, Token ident, Block block) {
        funcType = type;
        this.ident = ident;
        this.funcFParams = null;
        this.block = block;
        argc = 0;
    }

    public Token getIdent() {
        return ident;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public int getArgc() {
        return argc;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public boolean hasFParams() {
        return funcFParams != null;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String getSymbol() {
        return "<FuncDef>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(funcType);
        astNodes.add(funcFParams);
        astNodes.add(block);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        funcType.printToFile(bw);
        bw.write(ident + "\n");
        bw.write(TokenType.LPARENT + " (\n");
        if (funcFParams != null) {
            funcFParams.printToFile(bw);
        }
        bw.write(TokenType.RPARENT + " )\n");
        block.printToFile(bw);
        bw.write(getSymbol() + "\n");
    }
}
