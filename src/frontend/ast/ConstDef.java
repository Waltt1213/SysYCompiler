package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ConstDef implements AstNode {
    private final Token ident;
    private final ConstExp constExp;
    private final ConstInitVal constInitVal;

    public ConstDef(Token ident, ConstExp constExps, ConstInitVal constInitVal) {
        this.ident = ident;
        this.constExp = constExps;
        this.constInitVal = constInitVal;
    }

    public ConstDef(Token ident, ConstInitVal constInitVal) {
        this.ident = ident;
        this.constExp = null;
        this.constInitVal = constInitVal;
    }

    public Token getIdent() {
        return ident;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    public boolean hasArray() {
        return constExp != null;
    }

    @Override
    public String getSymbol() {
        return "<ConstDef>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(constExp);
        astNodes.add(constInitVal);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(ident.toString() + "\n");
        if (constExp != null) {
            bw.write(TokenType.LBRACK + " " + "[\n");
            constExp.printToFile(bw);
            bw.write(TokenType.RBRACK + " " + "]\n");
        }
        bw.write(TokenType.ASSIGN + " " + "=\n");
        constInitVal.printToFile(bw);
        bw.write(getSymbol() + "\n");
    }
}
