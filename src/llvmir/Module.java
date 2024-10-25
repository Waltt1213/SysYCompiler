package llvmir;

import llvmir.values.Function;
import llvmir.values.GlobalValue;

import java.util.ArrayList;
import java.util.LinkedList;

public class Module extends Value {
    private ArrayList<Function> functions;
    private LinkedList<GlobalValue> globalValues;

    public Module(TypeId vt, String name) {
        super(vt, name);
        functions = new ArrayList<>();
        globalValues = new LinkedList<>();
    }

    public void addFunction(Function func) {
        functions.add(func);
    }

    public void addGlobalValue(GlobalValue gv) {
        globalValues.add(gv);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Function function : functions) {
            sb.append(function.toString()).append('\n');
        }
        for (GlobalValue globalValue : globalValues) {
            sb.append(globalValue.toString()).append('\n');
        }
        return sb.toString();
    }
}
