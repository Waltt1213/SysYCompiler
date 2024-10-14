package table;

public class FuncVar extends Symbol {
    private final String funcName;

    public FuncVar(String name, SymType type, int depth,
                   int lineno, String funcName) {
        super(name, type, depth, lineno);
        this.funcName = funcName;
    }

    @Override
    public int getDepth() {
        return super.getDepth();
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
    public int getLineno() {
        return super.getLineno();
    }

    @Override
    public String getVarOrFunc() {
        return super.getVarOrFunc();
    }

    public String getFuncName() {
        return funcName;
    }
}
