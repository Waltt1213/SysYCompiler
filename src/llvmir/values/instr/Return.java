package llvmir.values.instr;

import llvmir.DataType;
import llvmir.Value;
import llvmir.values.BasicBlock;

public class Return extends Instruction {

    public Return(BasicBlock basicBlock) {
        super(DataType.VoidTy, Type.RET, basicBlock);
    }

    public Return(BasicBlock basicBlock, Value value) {
        super(value.getTp(), Type.RET, basicBlock);
        addOperands(value);
    }

    @Override
    public String toString() {
        if (tp.equals(DataType.VoidTy)) {
            return "ret void";
        } else {
            return "ret " + getOperands().get(0).toString();
        }
    }
}
