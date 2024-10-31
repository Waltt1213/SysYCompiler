package llvmir.values;

import llvmir.DataType;
import llvmir.User;

public class Constant extends User {
    public Constant(DataType vt, String name) {
        super(vt, name);
    }
}
