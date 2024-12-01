package middle.optimizer;

import llvmir.Module;
import llvmir.values.BasicBlock;
import llvmir.values.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class SimplifyBlock {
    private final Module module;

    public SimplifyBlock(Module module) {
        this.module = module;
    }

    // 删除死代码块
    public void removeDeadBlocks() {
        for (Function function : module.getFunctions()) {
            dfsUnableReachBlock(function);
        }
    }

    private void dfsUnableReachBlock(Function function) {
        ArrayList<BasicBlock> allBlocks = function.getBasicBlocks();
        HashSet<BasicBlock> reached = new HashSet<>();
        BasicBlock first = allBlocks.get(0);
        dfs(first, reached);
        // 得到的是能访问到的基本块
        ArrayList<BasicBlock> removedBlocks = new ArrayList<>(allBlocks);
        removedBlocks.removeAll(reached);
        for (BasicBlock removed: removedBlocks) {
            function.removeBasicBlock(removed);
        }
    }

    private void dfs(BasicBlock start,
                     HashSet<BasicBlock> reached) {
        if (reached.contains(start)) {
            return;
        }
        reached.add(start);
        for (BasicBlock basicBlock: start.getSubsequents()) {
            dfs(basicBlock, reached);
        }
    }
}
