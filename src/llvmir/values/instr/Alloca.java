package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.Constant;

import java.util.ArrayList;

public class Alloca extends Instruction {
    // 传递进来的vt是ArrayType
    private boolean isConst = false;
    private ArrayList<Value> constInits;

    public Alloca(ValueType.Type vt, String name) {
        super(vt, Type.ALLOCA, name);
        constInits = new ArrayList<>();
    }

    public ValueType.DataType getDataType() {
        return tp.getDataType();
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public boolean isArray() {
        return tp.getDim() > 0;
    }

    public void addConstInit(Value value) {
        constInits.add(value);
    }

    public ArrayList<Value> getConstInits() {
        return constInits;
    }

    public ValueType.Type getElementType() {
        return new ValueType.Type(tp.getDataType());
    }

    public Value getInit(int bis) {
        if (bis < constInits.size()) {
            return constInits.get(bis);
        }
        return new Constant(getElementType(),"0");
    }

    public int getDim() {
        return tp.getDim();
    }

    @Override
    public ValueType.Type getTp() {
        return new ValueType.PointerType(tp);
    }

    @Override
    public String toString() {
        return getFullName() + " = alloca " + getTp().getActType().toString();
    }
}
