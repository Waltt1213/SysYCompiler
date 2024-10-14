package frontend.ast;

import frontend.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class UnaryOp implements AstNode {
    private final Token unaryOp;

    public UnaryOp(Token unaryOp) {
        this.unaryOp = unaryOp;
    }

    public int getLineno() {
        return unaryOp.getLineno();
    }

    @Override
    public String getSymbol() {
        return "<UnaryOp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>();
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(unaryOp + "\n");
        bw.write(getSymbol() + "\n");
    }

    @Override
    public String toString() {
        return unaryOp.toString();
    }
}
