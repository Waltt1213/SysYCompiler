package llvmir.values;

import llvmir.TypeId;
import llvmir.Value;

public class Constant extends Value {
    public Constant(TypeId vt, String name) {
        super(vt, name);
    }
}
