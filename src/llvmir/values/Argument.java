package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;

public class Argument extends Value {
    private boolean needName;

    public Argument(ValueType.Type vt, String name) {
        super(vt, name);
        id = localId;
    }

    public boolean isNeedName() {
        return needName;
    }

    public void setNeedName(boolean needName) {
        this.needName = needName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
