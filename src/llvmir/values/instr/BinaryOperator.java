package llvmir.values.instr;

import llvmir.DataType;
import llvmir.Value;

public class BinaryOperator extends Instruction {

    public BinaryOperator(DataType vt, Type type, String name) {
        super(vt, type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Value value1 = getOperands().get(0);
        Value value2 = getOperands().get(1);
        sb.append(id).append(name).append(" = ");
        sb.append(getIrType());
        sb.append(value1.getDef()).append(", ").append(value2.getName());
        return sb.toString();
    }
}
