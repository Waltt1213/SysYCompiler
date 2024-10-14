package table;

import frontend.ast.FuncFParams;

public class Function extends Symbol {
    private int argc;
    private FuncFParams funcFParams;

    public Function(String name, SymType type, int depth,
                    int lineno, int argc) {
        super(name, type, depth, lineno);
        this.argc = argc;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public void setFuncFParams(FuncFParams funcFParams) {
        this.funcFParams = funcFParams;
    }

    public void setArgc(int argc) {
        this.argc = argc;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public SymType getType() {
        return super.getType();
    }

    @Override
    public int getDepth() {
        return super.getDepth();
    }

    @Override
    public int getLineno() {
        return super.getLineno();
    }

    @Override
    public String getVarOrFunc() {
        return "Func";
    }

    public int getArgc() {
        return argc;
    }
}
