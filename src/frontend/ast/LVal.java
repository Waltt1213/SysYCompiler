package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LVal implements AstNode {
    private final Token ident;
    private final Exp exp;

    public LVal(Token ident, Exp exp) {
        this.exp = exp;
        this.ident = ident;
    }

    public LVal(Token ident) {
        this.exp = null;
        this.ident = ident;
    }

    public int getLineno() {
        return ident.getLineno();
    }

    public Token getIdent() {
        return ident;
    }

    public String getIdentName() {
        return ident.getContent();
    }

    public boolean isArray() {
        return exp != null;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public String getSymbol() {
        return "<LVal>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(exp);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(ident + "\n");
        if (exp != null) {
            bw.write(TokenType.LBRACK + " [\n");
            exp.printToFile(bw);
            bw.write(TokenType.RBRACK + " ]\n");
        }
        bw.write(getSymbol() + "\n");
    }
}
