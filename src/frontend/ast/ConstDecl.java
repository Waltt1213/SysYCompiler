package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ConstDecl implements AstNode {
    private final Token type;
    private final ArrayList<ConstDef> constDefs;

    public ConstDecl(Token type, ArrayList<ConstDef> constDefs) {
        this.type = type;
        this.constDefs = constDefs;
    }

    public Token getType() {
        return type;
    }

    @Override
    public String getSymbol() {
        return "<ConstDecl>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(constDefs);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        String constTk = TokenType.CONSTTK + " " + "const" + "\n";
        bw.write(constTk);
        bw.write(type + "\n");
        constDefs.get(0).printToFile(bw);
        String comma = TokenType.COMMA + " " + "," + "\n";
        for (int i = 1; i < constDefs.size(); i++) {
            bw.write(comma);
            constDefs.get(i).printToFile(bw);
        }
        String semicn = TokenType.SEMICN + " " + ";" + "\n";
        bw.write(semicn);
        bw.write(getSymbol() + "\n");
    }
}
