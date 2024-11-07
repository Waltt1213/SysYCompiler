package llvmir.values.instr;

import llvmir.ValueType;

public class Alloca extends Instruction {
    // 传递进来的vt是ArrayType
    public Alloca(ValueType.Type vt, String name) {
        super(vt, Type.ALLOCA, name);
    }

    public ValueType.DataType getDataType() {
        return tp.getDataType();
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
