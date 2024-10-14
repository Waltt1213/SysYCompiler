package frontend.ast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CompUnit implements AstNode {
    private final ArrayList<AstNode> decls;
    private final ArrayList<FuncDef> funcDefs;
    private final MainFuncDef mainFuncDef;

    public CompUnit(ArrayList<AstNode> decls, ArrayList<FuncDef> funcDefs,
                    MainFuncDef mainFuncDef) {
        this.decls = decls;
        this.funcDefs = funcDefs;
        this.mainFuncDef = mainFuncDef;
    }

    @Override
    public String getSymbol() {
        return "<CompUnit>";
    }

    @Override
    public ArrayList<AstNode> getAstChild() {
        ArrayList<AstNode> astNodes = new ArrayList<>();
        astNodes.addAll(decls);
        astNodes.addAll(funcDefs);
        astNodes.add(mainFuncDef);
        return astNodes;
    }

    @Override
    public void printToFile(BufferedWriter bw) throws IOException {
        for (AstNode decl : decls) {
            decl.printToFile(bw);
        }
        for (FuncDef funcDef : funcDefs) {
            funcDef.printToFile(bw);
        }
        if (mainFuncDef != null) {
            mainFuncDef.printToFile(bw);
        }
        bw.write(getSymbol() + "\n");
    }
}
