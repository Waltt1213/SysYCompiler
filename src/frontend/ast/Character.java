package frontend.ast;

import frontend.Token;
import utils.Transform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Character implements AstNode {
    private final Token charConst;

    public Character(Token charConst) {
        this.charConst = charConst;
    }

    public int getLineno() {
        return charConst.getLineno();
    }

    public String getChar() {
        int ascii = Transform.str2int(charConst.getContent().substring(1, charConst.getContent().length() - 1));
        return String.valueOf(ascii);
    }

    @Override
    public String getSymbol() {
        return "<Character>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>();
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(charConst + "\n");
        bw.write(getSymbol() + "\n");
    }
}
