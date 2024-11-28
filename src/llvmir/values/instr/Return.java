package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;

public class Return extends Instruction {

    public Return(String name) {
        super(new ValueType.Type(ValueType.DataType.VoidTy), Type.RET, name);
    }

    public Return(String name, Value value) {
        super(value.getTp(), Type.RET, name);
        addOperands(value);
    }

    @Override
    public Value def() {
        return null;
    }

    @Override
    public String toString() {
        if (tp.getDataType().equals(ValueType.DataType.VoidTy)) {
            return "ret void";
        } else {
            return "ret " + getOperands().get(0).getDef();
        }
    }
}
