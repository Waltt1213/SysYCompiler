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
public class ActiveAnalyze {
    private final ArrayList<Function> functions;

    public ActiveAnalyze(ArrayList<Function> functions) {
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
                calUseDef(block);
            }
        }
    }

    /**
     * 计算基本块的use def
     * @param basicBlock 基本块
     */
    private void calUseDef(BasicBlock basicBlock) {
        HashSet<Value> use = new HashSet<>();
        HashSet<Value> def = new HashSet<>();
        for (Instruction instr: basicBlock.getInstructions()) {
            for (Value operand: instr.getOperands()) {
                if (operand instanceof GlobalVariable
                        || operand instanceof Constant
                        || operand instanceof Alloca) {
                    continue;
                }
                use.add(operand);
            }

        }
    }

}
