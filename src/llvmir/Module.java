package llvmir;

import llvmir.values.Function;
import llvmir.values.GlobalVariable;

import java.util.ArrayList;
import java.util.LinkedList;

public class Module extends Value {
    private ArrayList<Function> functions;
    private LinkedList<GlobalVariable> globalValues;

    public Module(DataType vt, String name) {
        super(vt, name);
        functions = new ArrayList<>();
        globalValues = new LinkedList<>();
    }

    public void addFunction(Function func) {
        functions.add(func);
    }

    public void addGlobalValue(GlobalVariable gv) {
        globalValues.add(gv);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Function function : functions) {
            sb.append(function.toString()).append('\n');
        }
        for (GlobalVariable globalValue : globalValues) {
            sb.append(globalValue.toString()).append('\n');
        }
        return sb.toString();
    }
}
