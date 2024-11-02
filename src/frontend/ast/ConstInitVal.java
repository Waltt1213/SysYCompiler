package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ConstInitVal implements AstNode {
    private final ConstExp constExp;
    private final ArrayList<ConstExp> constExps;
    private final Token stringConst;

    public ConstInitVal(ConstExp constExp) {
        this.constExp = constExp;
        constExps = null;
        stringConst = null;
    }

    public ConstInitVal(ArrayList<ConstExp> constExps) {
        constExp = null;
        this.constExps = constExps;
        stringConst = null;
    }

    public ConstInitVal(Token stringConst) {
        constExp = null;
        constExps = null;
        this.stringConst = stringConst;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public ArrayList<ConstExp> getConstExps() {
        return constExps;
    }

    public Token getStringConst() {
        return stringConst;
    }

    public boolean isConstExp() {
        return constExp != null && constExps == null && stringConst == null;
    }

    public boolean isConstExps() {
        return constExp == null && constExps != null && stringConst == null;
    }

    public boolean isStringConst() {
        return constExp == null && constExps == null && stringConst != null;
    }

    @Override
    public String getSymbol() {
        return "<ConstInitVal>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        if (isConstExps()) {
            astNodes.addAll(constExps);
        } else if (isConstExp()) {
            astNodes.add(constExp);
        }
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        if (constExp != null) {
            constExp.printToFile(bw);
        }
        if (constExps != null) {
            bw.write(TokenType.LBRACE + " {\n");
            if (!constExps.isEmpty()) {
                constExps.get(0).printToFile(bw);
                for (int i = 1; i < constExps.size(); i++) {
                    bw.write(TokenType.COMMA + " ,\n");
                    constExps.get(i).printToFile(bw);
                }
            }
            bw.write(TokenType.RBRACE + " }\n");
        }
        if (stringConst != null) {
            bw.write(stringConst + "\n");
        }
        bw.write(getSymbol() + "\n");
    }
}
