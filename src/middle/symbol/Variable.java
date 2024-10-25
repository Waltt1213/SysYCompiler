package middle.symbol;

import frontend.ast.AstNode;
import frontend.ast.ConstInitVal;
import frontend.ast.InitVal;

public class Variable extends Symbol {
    private final AstNode initVal;

    public Variable(String name, SymType type, int depth,
                    int lineno, InitVal initVal) {
        super(name, type, depth, lineno);
        this.initVal = initVal;
    }

    public Variable(String name, SymType type, int depth,
                    int lineno, ConstInitVal constInitVal) {
        super(name, type, depth, lineno);
        this.initVal = constInitVal;
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
        return super.getVarOrFunc();
    }

    public boolean isConst() {
        return super.getType().isConst();
    }

    public boolean isArray() {
        return super.getType().isArray();
    }

    public boolean hasInitVal() {
        return initVal != null;
    }

}
