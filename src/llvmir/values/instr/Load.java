package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Load extends Instruction {

    public Load(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.LOAD, basicBlock);
    }
}
