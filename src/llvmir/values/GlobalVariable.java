package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

import java.util.ArrayList;

public class GlobalVariable extends Value {
    private ArrayList<Value> initVal;
    private Constant dim; // 维度

    public GlobalVariable(DataType vt, String name) {
        super(vt, name);
        id = globalId;
        initVal = new ArrayList<>();
    }

    public void setDim(Constant dim) {
        this.dim = dim;
    }

    public Constant getDim() {
        return dim;
    }

    public void setInitVal(ArrayList<Value> initVal) {
        this.initVal = initVal;
    }

    public ArrayList<Value> getInitVal() {
        return initVal;
    }

    public String getInits() {
        StringBuilder sb = new StringBuilder();
        if (initVal.size() == 1 && initVal.get(0).getTp() == DataType.Pointer8Ty) {
            String s = initVal.get(0).toString();
            sb.append("c").append(s, 0, s.length() - 1);
            for (int i = s.length() - 2; i < Integer.parseInt(dim.getName()); i++) {
                sb.append("\\00");
            }
            sb.append("\"");
        } else {
            sb.append("[");
            for (int i = 0; i < Integer.parseInt(dim.getName()); i++) {
                if (i < initVal.size()) {
                    sb.append(initVal.get(i).getDef());
                } else {
                    sb.append(tp.toString(), 0, tp.toString().length() - 1).append(" 0");
                }
                if (i < Integer.parseInt(dim.getName()) - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName()).append(" = dso_local global ");
        if (Integer.parseInt(dim.getName()) == 0) {
            if (initVal != null && !initVal.isEmpty()) {
                sb.append(tp).append(" ").append(initVal.get(0).getName());
            } else {
                sb.append(tp).append(" 0");
            }
        } else {
            sb.append("[").append(dim.getName()).append(" x ");
            sb.append(tp.toString(), 0, tp.toString().length() - 1).append("] ");
            if (initVal == null || initVal.isEmpty()) {
                sb.append("zeroinitializer");
            } else {
                sb.append(getInits());
            }
        }
        return sb.toString();
    }
}
