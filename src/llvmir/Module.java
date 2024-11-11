package llvmir;

import llvmir.values.Function;
import llvmir.values.GlobalVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Module extends Value {
    private HashMap<String, Function> declares;
    private ArrayList<Function> functions;
    private LinkedList<GlobalVariable> globalValues;

    public Module(ValueType.Type vt, String name) {
        super(vt, name);
        declares = new HashMap<>();
        functions = new ArrayList<>();
        globalValues = new LinkedList<>();
    }

    public void addDeclare(Function declare) {
        declares.put(declare.name, declare);
    }

    public void addFunction(Function func) {
        functions.add(func);
    }

    public void addGlobalValue(GlobalVariable gv) {
        globalValues.add(gv);
    }

    public Function getDeclare(String name) {
        if (declares.containsKey(name)) {
            return declares.get(name);
        }
        return null;
    }

    public LinkedList<GlobalVariable> getGlobalValues() {
        return globalValues;
    }

    public ArrayList<Function> getFunctions() {
        return functions;
    }

    public HashMap<String, Function> getDeclares() {
        return declares;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Function declare : declares.values()) {
            sb.append(declare.toString()).append('\n');
        }
        sb.append("\n");
        for (GlobalVariable globalValue : globalValues) {
            sb.append(globalValue.toString()).append('\n');
        }
        sb.append("\n");
        for (Function function : functions) {
            sb.append(function.toString()).append('\n');
        }
        return sb.toString();
    }
}
