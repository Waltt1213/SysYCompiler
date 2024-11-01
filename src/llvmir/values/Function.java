package llvmir.values;

import llvmir.DataType;
import llvmir.Module;
import llvmir.Value;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends Value {
    private ArrayList<Argument> funcFParams;
    private LinkedList<BasicBlock> basicBlocks;
    private final Module parent;
    private boolean isDefine;
    private boolean isReturn;

    public Function(DataType vt, String name, Module module,
                    boolean isDefine) {
        super(vt, name);
        basicBlocks = new LinkedList<>();
        parent = module;
        funcFParams = new ArrayList<>();
        this.isDefine = isDefine;
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

    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    public boolean isReturn() {
        return isReturn;
    }

    @Override
    public String getFullName() {
        return super.getFullName();
    }

    @Override
    public String getName() {
        return "@" + super.getName();
    }

    @Override
    public DataType getTp() {
        return super.getTp();
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
            sb.append(") {");
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
