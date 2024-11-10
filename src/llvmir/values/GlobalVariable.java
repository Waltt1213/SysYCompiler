package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import utils.Transform;

import java.util.ArrayList;

public class GlobalVariable extends Value {
    private ArrayList<Value> initVal;
    private boolean unnamed = false;
    private boolean isConstant = false;
    private boolean isString = false;

    public GlobalVariable(ValueType.Type vt, String name) {
        super(vt, name);
        id = globalId;
        initVal = new ArrayList<>();
    }

    public void setUnnamed(boolean unnamed) {
        this.unnamed = unnamed;
    }

    public void setConstant(boolean constant) {
        isConstant = constant;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public boolean isArray() {
        return tp.getDim() > 0;
    }

    public ValueType.DataType getDateType() {
        return tp.getDataType();
    }

    public void setString(boolean string) {
        isString = string;
    }

    public boolean isString() {
        return isString;
    }

    public void setInitVal(ArrayList<Value> initVal) {
        this.initVal = initVal;
        if (initVal.size() == 1 && initVal.get(0) instanceof Constant) {
            Constant constant = (Constant) initVal.get(0);
            if (constant.isString()) {
                setString(true);
            }
        }
        if (!isString()) {
            for (Value init: initVal) {
                if (init.getTp().getDataType() != tp.getDataType()) {
                    init.getTp().setDataType(tp.getDataType());
                }
            }
        }
    }

    public ArrayList<Value> getInitVal() {
        return initVal;
    }

    public String getInitValtoString() {
        StringBuilder sb = new StringBuilder();
        if (tp.getDim() == 0) { // 单独元素
            if (initVal != null && !initVal.isEmpty()) {
                sb.append(initVal.get(0).getName());
            } else {
                sb.append("0");
            }
        } else {    // 数组
            if (initVal == null || initVal.isEmpty()) {
                sb.append(0).append(":").append(tp.getDim());   // 全部填0
            } else {
                if (isString) {
                    sb.append("\"").append(initVal.get(0).getName()).append("\"");
                } else {
                    for (int i = 0; i < tp.getDim(); i++) {
                        if (i < initVal.size()) {
                            sb.append(initVal.get(i).getName());
                        } else {
                            sb.append(" 0");
                        }
                        if (i < tp.getDim() - 1) {
                            sb.append(", ");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public void addInitVal(Value value) {
        initVal.add(value);
    }

    public Value getInit(int bis) {
        if (isString) {
            String content = initVal.get(0).getName();
            if (bis < content.length()) {
                int init = Transform.str2int(String.valueOf(content.charAt(bis)));
                return new Constant(getElementType(), String.valueOf(init));
            }
            return new Constant(getElementType(),"0");
        }
        if (bis < initVal.size()) {
            return initVal.get(bis);
        }
        return new Constant(getElementType(),"0");
    }

    public ValueType.Type getElementType() {
        return new ValueType.Type(tp.getDataType());
    }

    @Override
    public ValueType.Type getTp() {
        return new ValueType.PointerType(tp);
    }

    public String getInits() {
        StringBuilder sb = new StringBuilder();
        if (isString) {
            ValueType.ArrayType type = (ValueType.ArrayType) tp;
            String s = Transform.charList2string(initVal.get(0).getName());
            // System.out.println(s);
            sb.append(" c\"").append(Transform.str2charList(s));
            for (int i = s.length(); i < type.getDim(); i++) {
                sb.append("\\00");
            }
            sb.append("\"");
        } else {
            sb.append(" [");
            for (int i = 0; i < tp.getDim(); i++) {
                if (i < initVal.size()) {
                    sb.append(initVal.get(i).getDef());
                } else {
                    sb.append(getElementType()).append(" 0");
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
            sb.append("private unnamed_addr ");
        } else {
            sb.append("dso_local ");
        }
        if (isConstant) {
            sb.append("constant ");
        } else {
            sb.append("global ");
        }
        if (tp.getDim() == 0) { // 只有一个元素
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
