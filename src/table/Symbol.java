package table;

public class Symbol {
    private final String name;
    private final SymType type;
    private final int depth;
    private final int lineno;

    public Symbol(String name, SymType type, int depth, int lineno) {
        this.depth = depth;
        this.name = name;
        this.type = type;
        this.lineno = lineno;
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
