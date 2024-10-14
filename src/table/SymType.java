package table;

public class SymType {
    private final String type; // int or char
    private final boolean isArray;
    private final boolean isConst;
    private final boolean isFunc;

    public SymType(String type, boolean isArray, boolean isConst, boolean isFunc) {
        this.type = type;
        this.isArray = isArray;
        this.isConst = isConst;
        this.isFunc = isFunc;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isFunc() {
        return isFunc;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isConst) {
            sb.append("Const");
        }
        sb.append(type);
        if (isArray) {
            sb.append("Array");
        }
        if (isFunc) {
            sb.append("Func");
        }
        return sb.toString();
    }
}
