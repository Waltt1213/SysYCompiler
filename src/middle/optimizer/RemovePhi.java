package middle.optimizer;

import llvmir.Module;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.instr.*;
import middle.SlotTracker;

import java.util.ArrayList;

public class RemovePhi {
    private Module module;

    public RemovePhi(Module module) {
        this.module = module;
    }

    public void removePhi() {
        phi2pc();
        pc2Move();
    }

    private void phi2pc() {
        for (Function function: module.getFunctions()) {
            phi2pcInFunc(function);
        }
    }

    private void phi2pcInFunc(Function function)  {
        ArrayList<BasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());
        for (BasicBlock basicBlock: blocks) {
            boolean hasPhi = false;
            for (Instruction instr: basicBlock.getInstructions()) {
                if (instr instanceof Phi) {
                    hasPhi = true;
                    break;
                }
            }
            if (!hasPhi) {
                continue;
            }
            ArrayList<Pc> pcs = new ArrayList<>();
            ArrayList<BasicBlock> pres = new ArrayList<>(basicBlock.getPrecursor());
            // 插入pc, 每个block的前序都会插入一个pc，负责多个并行的赋值（如果前驱向后继有多个跨基本块变量）
            // 每个前驱基本块和后继之前有且仅有这一个pc
            for (BasicBlock pre: pres) {
                Pc pc = new Pc();
                if (pre.getSubsequents().size() > 1) {
                    BasicBlock insert = new BasicBlock(SlotTracker.slot(), function);
                    insertBetween(pre, insert, basicBlock);
                    if (basicBlock.equals(pre.getNeighbour())) {
                        function.insertBlock(pre, insert);
                    } else {
                        function.addBasicBlock(insert);
                    }
                    insert.insertBeforeTerminator(pc);
                    pcs.add(pc);
                } else {
                    pre.insertBeforeTerminator(pc);
                    pcs.add(pc);
                }
            }
            // 为pc赋值,统计所有
            ArrayList<Instruction> instructions = new ArrayList<>(basicBlock.getInstructions());
            for (Instruction instruction: instructions) {
                if (!(instruction instanceof Phi)) {
                    continue;
                }
                Phi phi = (Phi) instruction;
                // pc集合是按前驱基本块顺序插入的，phi的操作数也是按前驱基本块顺序插入的，因此可以一一对应
                for (int i = 0; i < phi.getOperands().size(); i++) {
                    pcs.get(i).addOperands(phi, phi.getOperands().get(i));
                }
                basicBlock.getInstructions().remove(phi);
            }
        }
    }

    private void pc2Move() {
        for (Function function: module.getFunctions()) {
            pc2MoveInFunc(function);
        }
    }

    private void pc2MoveInFunc(Function function) {
        for (BasicBlock block: function.getBasicBlocks()) {
            ArrayList<Pc> pcs = new ArrayList<>();
            ArrayList<Instruction> instructions = new ArrayList<>(block.getInstructions());
            for (Instruction instruction: instructions) {
                if (!(instruction instanceof Pc)) {
                    continue;
                }
                Pc pc = (Pc) instruction;
                pcs.add(pc);
                while (pc.isUseful()) {
                    boolean flag = false;
                    for (int i = 0; i < pc.getDst().size(); i++) {
                        // 对于 b<-a, 不存在c<-b
                        if (!pc.getSrc().contains(pc.getDst().get(i))) {
                            Move move = new Move(pc.getDst().get(i), pc.getSrc().get(i), block);
                            block.insertBeforeTerminator(move);
                            pc.getSrc().remove(i);
                            pc.getDst().remove(i);
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        // 选择a!=b 的 b<-a
                        for (int i = 0; i < pc.getDst().size(); i++) {
                            if (pc.getDst().get(i).equals(pc.getSrc().get(i))) {
                                continue;
                            }
                            Value value = new Value(pc.getDst().get(i).getTp(), SlotTracker.slot());
                            Move move = new Move(value, pc.getSrc().get(i), block);
                            block.insertBeforeTerminator(move);
                            pc.getSrc().set(i, value);
                        }
                    }
                }
            }
            // 移除所有pc
            for (Pc pc: pcs) {
                pc.remove();
            }
        }
    }

    private void insertBetween(BasicBlock pre, BasicBlock insert, BasicBlock next) {
        pre.getSubsequents().add(insert);
        pre.getSubsequents().remove(next);
        next.getPrecursor().remove(pre);
        next.getPrecursor().add(insert);
        insert.getPrecursor().add(pre);
        insert.getSubsequents().add(next);
        Instruction terminator = pre.getTerminator();
        if (terminator instanceof Branch) {
            Branch branch = (Branch) terminator;
            branch.replaceValue(insert, next);
        }
        Branch branch = new Branch("");
        branch.addOperands(next);
        insert.setTerminator(branch);
        insert.setLabeled(true);
    }

}
