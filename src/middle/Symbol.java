package middle;

import frontend.ast.FuncFParams;
import llvmir.Value;

public class Symbol {
    private final String name;
    private final SymType type;
    private Value value;
    private final int depth;
    private final int lineno;
    private FuncFParams funcFParams;

    public Symbol(String name, SymType type, int depth, int lineno) {
        this.depth = depth;
        this.name = name;
        this.type = type;
        this.lineno = lineno;
    }

    public void setFuncFParams(FuncFParams funcFParams) {
        this.funcFParams = funcFParams;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public SymType getType() {
        return type;
    }

    public String getVarOrFunc() {
        if (type.isFunc()) {
            return "Func";
        }
        return "Var";
    }

    public int getLineno() {
        return lineno;
    }

    @Override
    public String toString() {
        return name + " " + type;
    }
}
