package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FuncType implements AstNode {
    private final Token type;

    public FuncType(Token type) {
        this.type = type;
    }

    public Token getType() {
        return type;
    }

    @Override
    public String getSymbol() {
        return "<FuncType>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>();
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(type + "\n");
        bw.write(getSymbol() + "\n");
    }
}
