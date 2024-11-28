package middle.optimizer;

import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Constant;
import llvmir.values.Function;
import llvmir.values.GlobalVariable;
import llvmir.values.instr.Alloca;
import llvmir.values.instr.Instruction;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * 进行活跃变量分析，为后端优化服务
 */
public class LiveAnalyze {
    private final ArrayList<Function> functions;
    private boolean changed;

    public LiveAnalyze(ArrayList<Function> functions) {
        this.functions = new ArrayList<>(functions);
    }

    /**
     * 活跃变量分析
     */
    public void analyzeActiveVar() {
        for (Function function: functions) {
            ArrayList<BasicBlock> blocks = function.getBasicBlocks();
            // 基本块的数据流分析
            for (BasicBlock block: blocks) {
                calUseDef(block);   // 计算def use
            }
            // 计算指令间
            changed = true;
            while (changed) {
                changed = false;
                for (BasicBlock block: function.getBasicBlocks()) {
                    calInout(block);
                }
            }
        }
        output();
    }

    /**
     * 计算基本块的use def
     * @param basicBlock 基本块
     */
    private void calUseDef(BasicBlock basicBlock) {
        HashSet<Value> use = new HashSet<>();   // 使用在定义前的变量
        HashSet<Value> def = new HashSet<>();   // 定义在使用前的变量
        for (Instruction instr: basicBlock.getInstructions()) {
            Value defInstr = instr.def();
            for (Value operand: instr.use()) {
                if (operand instanceof GlobalVariable
                        || operand instanceof Constant
                        || operand instanceof Alloca
                        || def.contains(operand)) {
                    continue;
                }
                use.add(operand);
            }
            if (defInstr != null && !use.contains(defInstr)) {
                def.add(defInstr);
            }
        }
        basicBlock.setDefs(def);
        basicBlock.setUses(use);
    }

    private void calInout(BasicBlock block) {
        HashSet<Value> in = new HashSet<>(block.getUses());
        HashSet<Value> out = new HashSet<>();
        // 计算out
        for (BasicBlock next: block.getSubsequents()) {
            out.addAll(next.getIns());
        }
        if (!out.equals(block.getOuts())) {
            block.setOuts(out);
        }
        // 计算in
        for (Value o: out) {
            if (!block.getDefs().contains(o)) {
                in.add(o);
            }
        }
        if (!in.equals(block.getIns())) {
            block.setIns(in);
            changed = true;
        }
    }

    private void output() {
        for (Function function : functions) {
            System.out.println("\n" + function.getFullName() + ":");
            ArrayList<BasicBlock> bbs = function.getBasicBlocks();
            for (BasicBlock bb : bbs) {
                HashSet<Value> use = bb.getUses();
                System.out.print("\n" + bb.getFullName() + ": use: ");
                for (Value value : use) {
                    System.out.print(value.getFullName() + " ");
                }
                HashSet<Value> def = bb.getDefs();
                System.out.print("\n" + bb.getFullName() + ": def: ");
                for (Value value : def) {
                    System.out.print(value.getFullName() + " ");
                }
                HashSet<Value> in = bb.getIns();
                System.out.print("\n" + bb.getFullName() + ": in: ");
                for (Value value : in) {
                    System.out.print(value.getFullName() + " ");
                }
                HashSet<Value> out = bb.getOuts();
                System.out.print("\n" + bb.getFullName() + ": out: ");
                for (Value value : out) {
                    System.out.print(value.getFullName() + " ");
                }
            }
        }
    }
}
