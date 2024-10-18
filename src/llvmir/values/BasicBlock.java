package llvmir.values;

import llvmir.TypeId;
import llvmir.Value;

public class BasicBlock extends Value {
    public BasicBlock(TypeId vt, String name) {
        super(vt, name);
    }
}
