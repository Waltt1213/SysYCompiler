package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class VarDecl implements AstNode {
    private final Token type;
    private final ArrayList<VarDef> varDefs;

    public VarDecl(Token type, ArrayList<VarDef> varDefs) {
        this.type = type;
        this.varDefs = varDefs;
    }

    public Token getType() {
        return type;
    }

    @Override
    public String getSymbol() {
        return "<VarDecl>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        return new ArrayList<>(varDefs);
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        bw.write(type + "\n");
        varDefs.get(0).printToFile(bw);
        for (int i = 1; i < varDefs.size(); i++) {
            bw.write(TokenType.COMMA + " ,\n");
            varDefs.get(i).printToFile(bw);
        }
        bw.write(TokenType.SEMICN + " ;\n");
        bw.write(getSymbol() + "\n");
    }
}
