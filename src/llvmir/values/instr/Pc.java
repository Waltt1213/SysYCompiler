package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;

import java.util.ArrayList;
import java.util.HashSet;

public class Pc extends Instruction {
    private ArrayList<Value> dst = new ArrayList<>();
    private ArrayList<Value> src = new ArrayList<>();

    public Pc() {
        super(new ValueType.Type(ValueType.DataType.VoidTy), Type.PC, "");
    }

    public void addOperands(Value left, Value right) {
        dst.add(left);
        src.add(right);
        addOperands(left);
        addOperands(right);
    }

    public ArrayList<Value> getDst() {
        return dst;
    }

    public ArrayList<Value> getSrc() {
        return src;
    }

    public boolean isUseful() {
        for (int i = 0; i < dst.size(); i++) {
            if (!dst.get(i).equals(src.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Value value: dst) {
            sb.append(value.getFullName()).append(" ");
        }
        sb.append("= pc ");
        for (Value value: src) {
            sb.append(value.getFullName()).append(" ");
        }
        return sb.toString();
    }
}
