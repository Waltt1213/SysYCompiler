package middle.optimizer;

import llvmir.Module;
import llvmir.User;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.instr.*;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

public class GCM {
    private Module module;
    private final HashSet<Instruction> pinned = new HashSet<>();
    private final HashMap<Instruction, BasicBlock> earlyMap = new HashMap<>();
    private final HashMap<Instruction, BasicBlock> lateMap = new HashMap<>();

    public GCM(Module module) {
        this.module = module;
    }

    public void gcm() {
        for (Function function: module.getFunctions()) {
            gcmForFunc(function);
        }
    }

    private void gcmForFunc(Function function) {
        for (BasicBlock block: function.getBasicBlocks()) {
            if (block instanceof BasicBlock.ForBlock) {
                ((BasicBlock.ForBlock) block).calLoopDepth();
            }
        }
        getPinned(function);
        HashSet<Instruction> analyzed = new HashSet<>(pinned);
        earlyMap.clear();
        for (Instruction  instruction: pinned) {
            earlyMap.put(instruction, instruction.getParent());
        }
        for (BasicBlock block: function.getBasicBlocks()) {
            for (Instruction instruction: block.getInstructions()) {
                if (!analyzed.contains(instruction)) {
                    scheduleEarly(instruction, analyzed);
                } else if (pinned.contains(instruction)) {
                    // 不可移动，分析其操作数
                    for (Value operand: instruction.use()) {
                        if (operand instanceof Instruction
                                && !analyzed.contains((Instruction) operand)) {
                            scheduleEarly((Instruction) operand, analyzed);
                        }
                    }
                }
            }
        }
        analyzed = new HashSet<>(pinned);
        lateMap.clear();
        for (Instruction  instruction: pinned) {
            lateMap.put(instruction, instruction.getParent());
        }
        for (int i = function.getBasicBlocks().size() - 1; i >= 0; i--) {
            BasicBlock block = function.getBasicBlocks().get(i);
            for (int j = block.getInstructions().size() - 1; j >= 0; j--) {
                Instruction instruction = block.getInstructions().get(j);
                if (!analyzed.contains(instruction)) {
                    scheduleLate(instruction, analyzed);
                } else if (pinned.contains(instruction)) {
                    for (User user: instruction.getUsersList()) {
                        if (user instanceof Instruction
                                && !analyzed.contains((Instruction) user)) {
                            scheduleLate((Instruction) user, analyzed);
                        }
                    }
                }
            }
        }
        // output(function);
    }

    private void getPinned(Function function) {
        pinned.clear();
        for (BasicBlock block : function.getBasicBlocks()) {
            for (Instruction inst : block.getInstructions()) {
                if (inst instanceof Call || inst instanceof Load || inst instanceof Store ||
                        inst instanceof Phi || inst instanceof Branch || inst instanceof Return) {
                    pinned.add(inst);
                }
            }
        }
    }

    private void scheduleEarly(Instruction instruction, HashSet<Instruction> analyzed) {
        // 先分析指令操作数，最后再分析指令
        final ArrayDeque<Instruction> stack = new ArrayDeque<>();
        stack.push(instruction);
        while (!stack.isEmpty()) {
            Instruction instr = stack.peek();
            boolean flag = true;
            for (Value operand: instr.use()) {
//                if (operand instanceof Instruction && stack.contains((Instruction) operand)) {
//                    continue;
//                }
                if (operand instanceof Instruction && !analyzed.contains((Instruction) operand)) {
                    stack.push((Instruction) operand);
                    flag = false;
                }
            }
            // flag == true 表示所有操作数都处理好了
            if (flag) {
                stack.pop();
                if (!analyzed.contains(instr)) {
                    analyzeEarliest(instr, analyzed);
                }
            }
        }
    }

    private void analyzeEarliest(Instruction instruction, HashSet<Instruction> analyzed) {
        if (!analyzed.add(instruction)) {
            throw new RuntimeException("Already analyzed");
        }
        earlyMap.put(instruction, instruction.getParent().getParent().getBasicBlocks().get(0));
        for (Value operand: instruction.use()) {
            if (operand instanceof Instruction) {
                Instruction o = (Instruction) operand;
                if (!analyzed.contains(o)) {
                    // 如果操作数还没有分析过，说明上一步出问题了
                    throw new RuntimeException("Not analyzed");
                }
                // 如果操作数的early在指令后，那就需要把指令后移到与操作数同一深度
                if (earlyMap.get(o).getDomDepth() > earlyMap.get(instruction).getDomDepth()) {
                    earlyMap.put(instruction, earlyMap.get(o));
                }
            }
        }
    }

    private void scheduleLate(Instruction instruction, HashSet<Instruction> analyzed) {
        final ArrayDeque<Instruction> stack = new ArrayDeque<>();
        stack.push(instruction);
        while (!stack.isEmpty()) {
            Instruction instr = stack.peek();
            boolean flag = true;
            for (User user: instr.getUsersList()) {
                if (user instanceof Instruction && !analyzed.contains(user)) {
                    stack.push((Instruction) user);
                    flag = false;
                }
            }
            // flag == true 表示所有操作数都处理好了
            if (flag) {
                stack.pop();
                if (!analyzed.contains(instr)) {
                    analyzeLatest(instr, analyzed);
                }
            }
        }
    }

    /**
     * @param block1 基本块1
     * @param block2 基本块2
     * @return 最低共同祖先
     */
    private BasicBlock findLCA(BasicBlock block1, BasicBlock block2) {
        if (block1 == null) {
            return block2;
        }
        BasicBlock a = block1;
        BasicBlock b = block2;
        // 平衡ab深度
        while (a.getDomDepth() > b.getDomDepth()) {
            a = a.getiDom();
        }
        while (b.getDomDepth() > a.getDomDepth()) {
            b = b.getiDom();
        }
        // 同时上移，寻找LCA
        while (!a.equals(b)) {
            a = a.getiDom();
            b = b.getiDom();
        }
        return a;
    }

    private void analyzeLatest(Instruction instruction, HashSet<Instruction> analyzed) {
        if (!analyzed.add(instruction)) {
            throw new RuntimeException("Already analyzed");
        }
        BasicBlock lca = null;
        for (User user: instruction.getUsersList()) {
            if (user instanceof Instruction) {
                Instruction o = (Instruction) user;
                if (!analyzed.contains(o)) {
                    throw new RuntimeException("Not analyzed");
                }
                // 如果操作数的early在指令后，那就需要把指令后移到与操作数同一深度
                if (o instanceof Phi) {
                    HashSet<BasicBlock> userBlocks = ((Phi) o).getPreBlockFromInstr(instruction);
                    for (BasicBlock userBlock: userBlocks) {
                        lca = findLCA(lca, userBlock);
                    }
                } else {
                    lca = findLCA(lca, o.getParent());
                }
            }
        }
        if (lca == null) {
            return;
            // throw new RuntimeException("No user inst!");
        }
        selectBlock(lca, instruction);
    }

    private void selectBlock(BasicBlock lca, Instruction instruction) {
        BasicBlock best = lca;
        BasicBlock newParent = lca;
        while (newParent != earlyMap.get(instruction)) {
            newParent = newParent.getiDom();
            if (newParent == null) {
                break;
            }
            if (newParent.getLoopDepth() < best.getLoopDepth()) {
                best = newParent;
            }
        }
        lateMap.put(instruction, best);
        if (best != null && !lateMap.get(instruction).equals(instruction.getParent())) {
            instruction.getParent().getInstructions().remove(instruction);
            BasicBlock target = lateMap.get(instruction);
            target.insertBeforeInstr(findInsertBefore(instruction, target), instruction);
        }
    }

    private Instruction findInsertBefore(Instruction instruction, BasicBlock block) {
        for (Instruction instr: block.getInstructions()) {
            if (instruction.getUsersList().contains(instr) && !(instr instanceof Phi)) {
                return instr;
            }
        }
        return block.getTerminator();
    }

    private void output(Function function) {
        System.out.println(function.getName() + " loops:");
        System.out.println("\nearly:");
        for (Instruction instruction: earlyMap.keySet()) {
            System.out.println(instruction.toString() + " early belongs to: " + earlyMap.get(instruction).getFullName());
        }
        System.out.println("\nlate:");
        for (Instruction instruction: lateMap.keySet()) {
            System.out.println(instruction.toString() + " late belongs to: " + lateMap.get(instruction).getFullName());
        }
    }
}
