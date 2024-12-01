package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Branch;
import llvmir.values.instr.Instruction;
import llvmir.values.instr.Pc;
import llvmir.values.instr.Phi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private final Function parent;
    private boolean needName = false;   // 需要SlotTracker提供一个名字
    private boolean isLabeled;  // 需要打印出名字
    private boolean isTerminator;
    private Instruction terminator;
    private final HashSet<BasicBlock> subsequents = new HashSet<>();
    private final HashSet<BasicBlock> precursor = new HashSet<>();
    private HashSet<BasicBlock> dom = new HashSet<>();  // 被支配的基本块集合
    private final HashSet<BasicBlock> domChild = new HashSet<>();
    private HashSet<BasicBlock> DF = new HashSet<>();
    private BasicBlock directDom;
    private HashSet<Value> uses = new HashSet<>();
    private HashSet<Value> defs = new HashSet<>();
    private HashSet<Value> ins = new HashSet<>();
    private HashSet<Value> outs = new HashSet<>();
    private BasicBlock neighbour;

    public BasicBlock(String name, Function function) {
        super(new ValueType.Type(ValueType.DataType.LabelTy), name);
        instructions = new LinkedList<>();
        parent = function;
        dom.add(this);
        directDom = null;
    }

    public void setNeedName(boolean needName) {
        this.needName = needName;
    }

    public boolean isNeedName() {
        return needName;
    }

    public void setLabeled(boolean labeled) {
        isLabeled = labeled;
    }

    public boolean isLabeled() {
        return isLabeled;
    }

    public String getLabel() {
        return "_" + parent.getName() + "_B" + getName();
    }

    public Function getParent() {
        return parent;
    }

    public void setName(String name) {
        super.setName(name);
    }

    public void addPreBlock(BasicBlock basicBlock) {
        precursor.add(basicBlock);  // basicBlock -> this
        basicBlock.addSubBlock(this);
    }

    public void addSubBlock(BasicBlock basicBlock) {
        subsequents.add(basicBlock);
    }

    public void removeSubBlock(BasicBlock basicBlock) {
        subsequents.remove(basicBlock);
    }

    public void removePreBlock(BasicBlock basicBlock) {
        precursor.remove(basicBlock);
    }

    public void setDom(HashSet<BasicBlock> dom) {
        this.dom = dom;
    }

    public HashSet<BasicBlock> getDom() {
        return dom;
    }

    public void calDirectDom() {
        int maxDom = 0;
        for (BasicBlock block: dom) {
            if (!block.equals(this)) {
                if (block.getDom().size() > maxDom) {
                    maxDom = block.getDom().size();
                    directDom = block;
                }
            }
        }
        if (directDom != null) {
            directDom.addDomChild(this);
        }
    }

    public void addDomChild(BasicBlock block) {
        domChild.add(block);
    }

    public HashSet<BasicBlock> getDomChild() {
        return domChild;
    }

    public BasicBlock getDirectDom() {
        return directDom;
    }

    public boolean isDomBy(BasicBlock basicBlock) {   // basicBlock 支配 this
        return dom.contains(basicBlock);
    }

    public boolean isStrictDomBy(BasicBlock basicBlock) {
        return dom.contains(basicBlock) && !this.equals(basicBlock);
    }

    public HashSet<BasicBlock> getDF() {
        return DF;
    }

    public void setDF(HashSet<BasicBlock> DF) {
        this.DF = DF;
    }

    public void setNeighbour(BasicBlock basicBlock) {
        neighbour = basicBlock;
    }

    public BasicBlock getNeighbour() {
        return neighbour;
    }

    public void appendInstr(Instruction instr, boolean setName) {
        if (!isTerminator) {
            instr.setNeedName(setName);
            instructions.add(instr);
            instr.setParent(this);
            if (instr instanceof Branch) {
                Branch branch = (Branch) instr;
                if (branch.getOperands().size() == 1) {
                    ((BasicBlock) branch.getOperands().get(0)).addPreBlock(this);
                } else if (branch.getOperands().size() == 3) {
                    ((BasicBlock) branch.getOperands().get(1)).addPreBlock(this);
                    ((BasicBlock) branch.getOperands().get(2)).addPreBlock(this);
                }
            }
            return;
        }
        for (Value operand: instr.getOperands()) {
            operand.removeUser(instr);
        }
    }

    public void removeInstr(Instruction instr) {
        instructions.remove(instr);
    }

    public void remove() {
        super.remove();
        while (!instructions.isEmpty()) {
            instructions.get(0).remove();
        }
    }

    public Phi insertPhi(Value value) {
        Phi phi = new Phi(value, "");
        phi.setNeedName(true);
        phi.setParent(this);
        phi.setPreBlocks(new ArrayList<>(this.getPrecursor()));
        instructions.add(0, phi);
        return phi;
    }

    public void insertBeforeTerminator(Instruction pc) {
        pc.setParent(this);
        if (isTerminator) {
            instructions.add(instructions.size() - 1, pc);
        } else {
            instructions.add(pc);
        }
    }

    public void setTerminator(Instruction branch) {
        // 优化不必要的条件跳转
        if (branch instanceof Branch && branch.getOperands().size() == 3) {
            Branch br = (Branch) branch;
            Value judge = br.getOperands().get(0);
            Value trueBlock = br.getOperands().get(1);
            Value falseBlock = br.getOperands().get(2);
            if (judge instanceof Constant) {
                if (judge.getName().equals("0")) {  // 永远为假
                    br.removeOperands(trueBlock);
                    br.removeOperands(judge);
                } else {    // 永远为真
                    br.removeOperands(falseBlock);
                    br.removeOperands(judge);
                }
            }
        }
        appendInstr(branch, false);
        terminator = branch;
        isTerminator = true;
    }

    public void setTerminator(boolean terminator) {
        isTerminator = terminator;
    }

    public Instruction getTerminator() {
        return terminator;
    }

    public boolean isTerminator() {
        return isTerminator;
    }

    public void setVirtualName() {
        for (Instruction instruction: instructions) {
            instruction.setVirtualName();
        }
    }

    public LinkedList<Instruction> getInstructions() {
        return instructions;
    }

    public HashSet<BasicBlock> getSubsequents() {
        return subsequents;
    }

    public HashSet<BasicBlock> getPrecursor() {
        return precursor;
    }

    public void setDefs(HashSet<Value> def) {
        this.defs = def;
    }

    public void setUses(HashSet<Value> use) {
        this.uses = use;
    }

    public void setIns(HashSet<Value> ins) {
        this.ins = ins;
    }

    public HashSet<Value> getOuts() {
        return outs;
    }

    public HashSet<Value> getUses() {
        return uses;
    }

    public HashSet<Value> getDefs() {
        return defs;
    }

    public HashSet<Value> getIns() {
        return ins;
    }

    public void setOuts(HashSet<Value> outs) {
        this.outs = outs;
    }

    @Override
    public String getDef() {
        return super.getDef();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isLabeled) {
            sb.append(getName()).append(": ");
            sb.append("\n");
        }
        if (!precursor.isEmpty()) {
            sb.append("; pred = ");
            for (BasicBlock basicBlock: precursor) {
                sb.append(basicBlock.getFullName()).append(", ");
            }
            sb.append("\n");
        }
        if (!subsequents.isEmpty()) {
            sb.append("; next = ");
            for (BasicBlock basicBlock: subsequents) {
                sb.append(basicBlock.getFullName()).append(", ");
            }
            sb.append("\n");
        }
        for (Instruction instr: instructions) {
            sb.append("\t").append(instr.toString()).append("\n");
        }
        return sb.toString();
    }

    public static class ForBlock extends BasicBlock {
        private BasicBlock outBlock;
        private BasicBlock updateBlock;

        public ForBlock(String name, BasicBlock outBlock, Function function) {
            super(name, function);
            this.outBlock = outBlock;
        }

        public void setOutBlock(BasicBlock outBlock) {
            this.outBlock = outBlock;
        }

        public void setUpdateBlock(BasicBlock updateBlock) {
            this.updateBlock = updateBlock;
        }

        public BasicBlock getOutBlock() {
            return outBlock;
        }

        public BasicBlock getUpdateBlock() {
            if (updateBlock == null) {
                return this;
            }
            return updateBlock;
        }
    }
}
