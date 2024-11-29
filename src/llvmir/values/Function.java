package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Return;
import utils.SlotTracker;

import java.util.ArrayList;

public class Function extends Value {
    private ArrayList<Argument> funcFParams;
    private ArrayList<BasicBlock> basicBlocks;
    private final boolean isDefine;
    private boolean isNotVoid;
    private boolean isReturn;

    public Function(ValueType.Type vt, String name,
                    boolean isDefine) {
        super(vt, name);
        basicBlocks = new ArrayList<>();
        funcFParams = new ArrayList<>();
        this.isDefine = isDefine;
        isNotVoid = false;
        isReturn = false;
        id = globalId;
    }

    public int getArgc() {
        return funcFParams.size();
    }

    public void addParams(Argument value, boolean needName) {
        funcFParams.add(value);
        value.setNeedName(needName);
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        if (!basicBlocks.isEmpty()) {
            BasicBlock last = basicBlocks.get(basicBlocks.size() - 1);
            // basicBlock.addPreBlock(last);
            last.setNeighbour(basicBlock);
        }
        basicBlocks.add(basicBlock);
        basicBlock.setNeedName(true);
    }

    public void insertBlock(BasicBlock pre, BasicBlock next) {
        if (!basicBlocks.isEmpty()) {
            pre.setNeighbour(next);
        }
        int index = basicBlocks.indexOf(pre);
        basicBlocks.add(index + 1, next);
        next.setNeedName(true);
    }

    public void removeBasicBlock(BasicBlock basicBlock) {
        if (basicBlocks.contains(basicBlock)) {
            // 删除后继联系
            for (BasicBlock next: basicBlock.getSubsequents()) {
                next.removePreBlock(basicBlock);
            }
            // 删除前驱联系
            for (BasicBlock pre: basicBlock.getPrecursor()) {
                pre.removeSubBlock(basicBlock);
            }
            basicBlocks.remove(basicBlock);
            basicBlock.remove();
        }
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

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(BasicBlock basicBlock) {
        Return ret = new Return(null);
        basicBlock.setTerminator(ret);
    }

    public void setReturn(Value value, BasicBlock basicBlock) {
        isReturn = true;
        Return ret = new Return(null, value);
        basicBlock.setTerminator(ret);
    }

    public void setVirtualName() {
        for (Argument argument: funcFParams) {
            if (argument.isNeedName()) {
                argument.setName(SlotTracker.slot());
            }
        }
        for (BasicBlock basicBlock: basicBlocks) {
            basicBlock.setName(SlotTracker.slot());
            basicBlock.setNeedName(false);
            basicBlock.setVirtualName();
        }
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
        return super.getName();
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
