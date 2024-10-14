package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FuncFParam implements AstNode {
    private final Token type;
    private final Token ident;
    private final boolean isArray;

    public FuncFParam(Token type, Token ident, boolean isArray) {
        this.type = type;
        this.ident = ident;
        this.isArray = isArray;
    }

    public Token getType() {
        return type;
    }

    public Token getIdent() {
        return ident;
    }

    public boolean isArray() {
        return isArray;
    }

    public String getIdentName() {
        return ident.getContent();
    }

    @Override
    public String getSymbol() {
        return "<FuncFParam>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>();
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(type + "\n");
        bw.write(ident + "\n");
        if (isArray) {
            bw.write(TokenType.LBRACK + " [\n");
            bw.write(TokenType.RBRACK + " ]\n");
        }
        bw.write(getSymbol() + "\n");
    }
}
