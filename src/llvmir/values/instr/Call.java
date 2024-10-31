package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Call extends Instruction {

    public Call(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.CALL, basicBlock);
    }
}
