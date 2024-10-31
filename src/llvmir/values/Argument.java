package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class Argument extends Value {
    public Argument(DataType vt, String name) {
        super(vt, name);
        id = localId;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
