package llvmir.values;

import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Return;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends Value {
    private ArrayList<Argument> funcFParams;
    private LinkedList<BasicBlock> basicBlocks;
    private final Module parent;
    private boolean isDefine;
    private boolean isNotVoid;
    private boolean isReturn;

    public Function(ValueType.Type vt, String name, Module module,
                    boolean isDefine) {
        super(vt, name);
        basicBlocks = new LinkedList<>();
        parent = module;
        funcFParams = new ArrayList<>();
        this.isDefine = isDefine;
        isNotVoid = false;
        isReturn = false;
        id = globalId;
    }

    public int getArgc() {
        return funcFParams.size();
    }

    public void addParams(Argument value) {
        funcFParams.add(value);
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlocks.add(basicBlock);
    }

    public void setNotVoid(boolean isReturn) {
        this.isNotVoid = isReturn;
    }

    public boolean isNotVoid() {
        return isNotVoid;
    }

    public ArrayList<Argument> getFuncFParams() {
        return funcFParams;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(BasicBlock basicBlock) {
        isReturn = true;
        Return ret = new Return(null);
        basicBlock.setTerminator(ret);
    }

    public void setReturn(Value value, BasicBlock basicBlock) {
        isReturn = true;
        Return ret = new Return(null, value);
        basicBlock.setTerminator(ret);
    }

    @Override
    public String getFullName() {
        return super.getFullName();
    }

    @Override
    public String getDef() {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.toString()).append(" ");
        sb.append(getFullName()).append("(");
        for (int i = 0; i < funcFParams.size(); i++) {
            sb.append(funcFParams.get(i).toString());
            if (i < funcFParams.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getName() {
        return "@" + super.getName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isDefine) {
            sb.append("define dso_local ").append(tp.toString()).append(" ");
            sb.append(getFullName()).append("(");
            for (int i = 0; i < funcFParams.size(); i++) {
                sb.append(funcFParams.get(i).toString());
                if (i < funcFParams.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(") {\n");
            for (BasicBlock basicBlock: basicBlocks) {
                sb.append(basicBlock.toString());
            }
            sb.append("}\n");
        } else {
            sb.append("declare ").append(tp.toString()).append(" ");
            sb.append(getFullName()).append("(");
            for (int i = 0; i < funcFParams.size(); i++) {
                sb.append(funcFParams.get(i).getTp().toString());
                if (i < funcFParams.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
