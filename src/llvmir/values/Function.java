package llvmir.values;

import llvmir.TypeId;
import llvmir.Value;

public class Function extends Value {
    public Function(TypeId vt, String name) {
        super(vt, name);
    }
}
