package middle.symbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import frontend.Error;

public class SymbolTable {
    private final LinkedHashMap<String, Symbol> symItems;
    private SymbolTable fatherTable;
    private final ArrayList<SymbolTable> children;
    private final int depth;

    public SymbolTable(SymbolTable fatherTable, int depth) {
        this.fatherTable = fatherTable;
        this.symItems = new LinkedHashMap<>();
        children = new ArrayList<>();
        this.depth = depth;
    }

    public void addSymItem(String name, Symbol sym) throws Error {
        if (symItems.containsKey(name)) {
            throw new Error("b", sym.getLineno());
        }
        symItems.put(name, sym);
    }

    public void addChildren(SymbolTable st) {
        children.add(st);
    }

    public ArrayList<SymbolTable> getChildren() {
        return children;
    }

    public boolean findSym(String name) {
        return symItems.containsKey(name);
    }

    public Symbol getSym(String name) {
        return symItems.get(name);
    }

    public void setFatherTable(SymbolTable fatherTable) {
        this.fatherTable = fatherTable;
    }

    public LinkedHashMap<String, Symbol> getSymItems() {
        return symItems;
    }

    public int SymNum() {
        return symItems.size();
    }

    public SymbolTable getFatherTable() {
        return fatherTable;
    }

    public int getDepth() {
        return depth;
    }
}
