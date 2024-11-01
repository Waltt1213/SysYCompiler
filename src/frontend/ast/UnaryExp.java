package frontend.ast;

import frontend.Token;
import frontend.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class UnaryExp implements AstNode {
    private final PrimaryExp primaryExp;
    private final Token ident;
    private final FuncRParams funcRParams;

    private final UnaryOp unaryOp;
    private final UnaryExp unaryExp;
    private final int argc;

    public UnaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
        ident = null;
        funcRParams = null;
        unaryExp = null;
        unaryOp = null;
        argc = 0;
    }

    public UnaryExp(Token ident) {
        primaryExp = null;
        this.ident = ident;
        funcRParams = null;
        unaryOp = null;
        unaryExp = null;
        argc = 0;
    }

    public UnaryExp(Token ident, FuncRParams funcRParams) {
        primaryExp = null;
        this.ident = ident;
        this.funcRParams = funcRParams;
        unaryExp = null;
        unaryOp = null;
        argc = funcRParams.getArgc();
    }

    public UnaryExp(UnaryOp unaryOp, UnaryExp unaryExp) {
        this.unaryOp = unaryOp;
        this.unaryExp = unaryExp;
        ident = null;
        funcRParams = null;
        primaryExp = null;
        argc = 0;
    }

    public int getArgc() {
        return argc;
    }

    public int getLineno() {
        if (ident != null) {
            return ident.getLineno();
        } else if (unaryOp != null) {
            return unaryOp.getLineno();
        } else {
            return primaryExp.getLineno();
        }
    }

    public String getIdentName() {
        if (ident != null) {
            return ident.getContent();
        } else if (primaryExp != null) {
            return primaryExp.getIdentName();
        } else {
            return unaryExp.getIdentName();
        }
    }

    public boolean isArray() {
        if (isPrimaryExp()) {
            return primaryExp.isArray();
        }
        return false;
    }

    public Token getIdent() {
        return ident;
    }

    public boolean isPrimaryExp() {
        return primaryExp != null;
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public boolean isIdent() {
        return ident != null;
    }

    public boolean hasFuncRParams() {
        return funcRParams != null;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public String getUnaryOp() {
        return unaryOp.getOp().getContent();
    }

    public boolean isUnary() {
        return unaryExp != null && unaryOp != null;
    }

    @Override
    public String getSymbol() {
        return "<UnaryExp>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        if (isPrimaryExp()) {
            astNodes.add(primaryExp);
        } else if (isIdent()) {
            if (hasFuncRParams()) {
                astNodes.add(funcRParams);
            }
        } else if (isPrimaryExp()) {
            astNodes.add(unaryOp);
            astNodes.add(unaryExp);
        }
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        if (primaryExp != null) {
            primaryExp.printToFile(bw);
        } else if (ident != null) {
            bw.write(ident + "\n");
            bw.write(TokenType.LPARENT + " (\n");
            if (funcRParams != null) {
                funcRParams.printToFile(bw);
            }
            bw.write(TokenType.RPARENT + " )\n");
        } else if (unaryOp != null && unaryExp != null) {
            unaryOp.printToFile(bw);
            unaryExp.printToFile(bw);
        }
        bw.write(getSymbol() + "\n");
    }
}
