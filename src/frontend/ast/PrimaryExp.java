package frontend.ast;

import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class PrimaryExp implements AstNode {
    private final AstNode primaryExp;

    public PrimaryExp(Exp exp) {
        primaryExp = exp;
    }

    public PrimaryExp(LVal lVal) {
        primaryExp = lVal;
    }

    public PrimaryExp(Number number) {
        primaryExp = number;
    }

    public PrimaryExp(Character character) {
        primaryExp = character;
    }

    public boolean isArray() {
        if (primaryExp instanceof LVal) {
            return ((LVal) primaryExp).isArrayElement();
        } else if (primaryExp instanceof Exp) {
            return ((Exp) primaryExp).isArray();
        }
        return false;
    }

    public AstNode getPrimaryExp() {
        return primaryExp;
    }

    public String getIdentName() {
        if (primaryExp instanceof LVal) {
            return ((LVal) primaryExp).getIdentName();
        } else if (primaryExp instanceof Exp) {
            return ((Exp) primaryExp).getIdentName();
        } else {
            return "";
        }
    }

    public int getLineno() {
        if (primaryExp instanceof LVal) {
            return ((LVal) primaryExp).getLineno();
        } else if (primaryExp instanceof Exp) {
            return ((Exp) primaryExp).getLineno();
        } else if (primaryExp instanceof Number) {
            return ((Number) primaryExp).getLineno();
        } else {
            return ((Character) primaryExp).getLineno();
        }
    }

    @Override
    public String getSymbol() {
        return "<PrimaryExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.add(primaryExp);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        if (primaryExp instanceof Exp) {
            bw.write(TokenType.LPARENT + " (\n");
            primaryExp.printToFile(bw);
            bw.write(TokenType.RPARENT + " )\n");
        } else if (primaryExp instanceof LVal) {
            primaryExp.printToFile(bw);
        } else if (primaryExp instanceof Number) {
            primaryExp.printToFile(bw);
        } else if (primaryExp instanceof Character) {
            primaryExp.printToFile(bw);
        }
        bw.write(getSymbol() + "\n");
    }
}
