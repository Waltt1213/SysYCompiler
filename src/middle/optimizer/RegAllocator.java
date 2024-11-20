package middle.optimizer;

import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.instr.Instruction;

import java.util.ArrayList;

/**
 * 进行活跃变量分析，为后端优化服务
 */
public class RegAllocator {
    private final ArrayList<Function> functions;

    public RegAllocator(ArrayList<Function> functions) {
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
        for (Instruction instr: basicBlock.getInstructions()) {

        }
    }

}
