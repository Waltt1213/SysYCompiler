package middle.optimizer;

import llvmir.Module;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Constant;
import llvmir.values.Function;
import llvmir.values.instr.*;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

public class Mem2reg {
    private Module module;
    HashMap<Value, HashSet<BasicBlock>> defs = new HashMap<>();
    HashMap<Value, HashSet<Instruction>> defInstrs = new HashMap<>();
    HashMap<Value, HashSet<BasicBlock>> uses = new HashMap<>();
    HashMap<Value, HashSet<Instruction>> useInstrs = new HashMap<>();

    public Mem2reg(Module module) {
        this.module = module;
    }

    public void buildSSA() {
        for (Function function: module.getFunctions()) {
            buildFuncSSA(function);
        }
    }

    private void buildFuncSSA(Function function) {
        HashSet<Instruction> allocas = new HashSet<>();
        // 先统计所有变量
        for (BasicBlock block: function.getBasicBlocks()) {
            for (Instruction instruction: block.getInstructions()) {
                // 只考虑非数组变量
                if (instruction instanceof Alloca && ((Alloca) instruction).getDim() == 0) {
                    allocas.add(instruction);
                    defs.put(instruction, new HashSet<>());
                    defInstrs.put(instruction, new HashSet<>());
                    uses.put(instruction, new HashSet<>());
                    useInstrs.put(instruction, new HashSet<>());
                } else if (instruction instanceof Store) {
                    if (defs.containsKey(instruction.getOperands().get(1))) {
                        defs.get(instruction.getOperands().get(1)).add(instruction.getParent());
                        defInstrs.get(instruction.getOperands().get(1)).add(instruction);
                    }
                } else if (instruction instanceof Load) {
                    if (uses.containsKey(instruction.getOperands().get(0))) {
                        uses.get(instruction.getOperands().get(0)).add(instruction.getParent());
                        useInstrs.get(instruction.getOperands().get(0)).add(instruction);
                    }
                }
            }
        }
        // 插入phi函数
        for (Instruction alloca: allocas) {
            // TODO:剪枝
            insertPhi(alloca, function.getBasicBlocks().get(0));
        }
    }

    public void insertPhi(Instruction alloca, BasicBlock first) {
        HashSet<BasicBlock> f = new HashSet<>();    // 需要插入phi的基本块集合
        HashSet<BasicBlock> w = new HashSet<>(defs.get(alloca)); // value的定义（包括phi）所在基本块集合
        while (!w.isEmpty()) {
            BasicBlock block = null;
            for (BasicBlock basicBlock: w) {
                block = basicBlock;
                break;
            }
            for (BasicBlock y: block.getDF()) {
                if (!f.contains(y)) {
                    // 插入phi;
                    Phi phi = y.insertPhi(alloca);
                    f.add(y);
                    if (!defs.get(alloca).contains(y)) {
                        w.add(y);
                    }
                    useInstrs.get(alloca).add(phi);
                    defInstrs.get(alloca).add(phi);
                }
            }
            w.remove(block);
        }
        ArrayDeque<Value> defStack = new ArrayDeque<>();
        defStack.push(new Constant(alloca.getTp().getInnerType(),"0"));
        DfsRename(defStack, first, alloca);
        // 删除不必要的alloca, store, load
        alloca.remove();
        for (Instruction instr: defInstrs.get(alloca)) {
            if (!(instr instanceof Phi)) {
                instr.remove();
            }
        }
        for (Instruction instr: useInstrs.get(alloca)) {
            if (!(instr instanceof Phi)) {
                instr.remove();
            }
        }
    }

    private void DfsRename(ArrayDeque<Value> defStack, BasicBlock block, Value alloca) {
        // defStack里总是最新的定义
        int cnt = 0;
        for (Instruction instr: block.getInstructions()) {
            if (defInstrs.get(alloca).contains(instr)) {
                if (instr instanceof Store) {
                    defStack.push(instr.getOperands().get(0));
                    cnt++;
                } else if (instr instanceof Phi) {
                    defStack.push(instr);
                    cnt++;
                }
            } else if (useInstrs.get(alloca).contains(instr) && !(instr instanceof Phi)) {
                Value value = defStack.peek();
                instr.replaceAllUses(value);
            }
        }
        for (BasicBlock basicBlock: block.getSubsequents()) {
            for (Instruction instr: basicBlock.getInstructions()) {
                if (instr instanceof Phi && useInstrs.get(alloca).contains(instr)) {
                    ((Phi) instr).replaceValue(defStack.peek(), block);
                } else {
                    break;
                }
            }
        }
        // 前序遍历
        for (BasicBlock basicBlock: block.getDomChild()) {
            DfsRename(defStack, basicBlock, alloca);
        }
        for (int i = 0; i < cnt; i++) {
            defStack.pop();
        }
    }

}
