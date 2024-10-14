package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FuncFParams implements AstNode {
    private final ArrayList<FuncFParam> funcFParams;
    private final int argc;
    private final String funcName;

    public FuncFParams(ArrayList<FuncFParam> funcFParams, String funcName) {
        this.funcFParams = funcFParams;
        argc = funcFParams.size();
        this.funcName = funcName;
    }

    public String getFuncName() {
        return funcName;
    }

    public int getArgc() {
        return argc;
    }

    public FuncFParam getParam(int i) {
        return funcFParams.get(i);
    }

    @Override
    public String getSymbol() {
        return "<FuncFParams>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(funcFParams);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        funcFParams.get(0).printToFile(bw);
        for (int i = 1; i < funcFParams.size(); i++) {
            bw.write(TokenType.COMMA + " ,\n");
            funcFParams.get(i).printToFile(bw);
        }
        bw.write(getSymbol() + "\n");
    }
}
