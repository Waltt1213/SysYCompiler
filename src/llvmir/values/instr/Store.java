package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Store extends Instruction {

    public Store(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.STORE, basicBlock);
    }
}
