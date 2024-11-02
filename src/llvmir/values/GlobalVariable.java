package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import utils.Transform;

import java.util.ArrayList;

public class GlobalVariable extends Value {
    private ArrayList<Value> initVal;
    private boolean unnamed = false;

    public GlobalVariable(ValueType.Type vt, String name) {
        super(vt, name);
        id = globalId;
        initVal = new ArrayList<>();
    }

    public void setUnnamed(boolean unnamed) {
        this.unnamed = unnamed;
    }

    public void setInitVal(ArrayList<Value> initVal) {
        this.initVal = initVal;
    }

    public ArrayList<Value> getInitVal() {
        return initVal;
    }

    public void addInitVal(Value value) {
        initVal.add(value);
    }

    @Override
    public ValueType.Type getTp() {
        return tp.getAddr();
    }

    public String getInits() {
        StringBuilder sb = new StringBuilder();
        if (initVal.get(0).getTp() instanceof ValueType.ArrayType
                && initVal.get(0).getTp().getDataType() == ValueType.DataType.Integer8Ty) {
            ValueType.ArrayType type = (ValueType.ArrayType) tp;
            String s = Transform.charList2string(initVal.get(0).getName());
            sb.append(" c\"").append(Transform.str2charList(s));
            for (int i = s.length(); i < type.getDim(); i++) {
                System.out.println(s.length());
                sb.append("\\00");
            }
            sb.append("\"");
        } else {
            sb.append(" [");
            for (int i = 0; i < tp.getDim(); i++) {
                if (i < initVal.size()) {
                    sb.append(initVal.get(i).getDef());
                } else {
                    sb.append(tp.toString(), 0, tp.toString().length() - 1).append(" 0");
                }
                if (i < tp.getDim() - 1) {
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
        sb.append(getFullName()).append(" = ");
        if (unnamed) {
            sb.append("private unnamed_addr constant ");
        } else {
            sb.append("dso_local global ");
        }
        if (tp.getDim() == 0) {
            if (initVal != null && !initVal.isEmpty()) {
                sb.append(tp).append(" ").append(initVal.get(0).getName());
            } else {
                sb.append(tp).append(" 0");
            }
        } else {
            sb.append(tp);
            if (initVal == null || initVal.isEmpty()) {
                sb.append(" zeroinitializer");
            } else {
                sb.append(getInits());
            }
        }
        return sb.toString();
    }
}
