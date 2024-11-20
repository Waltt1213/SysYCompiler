package middle.optimizer;

import llvmir.Module;
import llvmir.values.BasicBlock;
import llvmir.values.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class Optimizer {
    private Module module;
    private ArrayList<Function> functions;

    public Optimizer(Module module) {
        this.module = module;
        functions = module.getFunctions();
    }

    public void optimize() {
        module.setVirtualName();
        removeDeadBlocks();
        buildDom();
    }

    // 优化1：删除死代码块
    private void removeDeadBlocks() {
        for (Function function : functions) {
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

    private void buildDom() {
        for (Function function: functions) {
            buildFuncDom(function);
        }
    }

    private void buildFuncDom(Function function) {
        boolean changed;
        BasicBlock first = function.getBasicBlocks().get(0);
        for (BasicBlock block: function.getBasicBlocks()) {
            if (block.equals(first)) {
                continue;
            }
            HashSet<BasicBlock> initDom = new HashSet<>(function.getBasicBlocks());
            block.setDom(initDom);
        }
        do {
            changed = false;
            for (BasicBlock block: function.getBasicBlocks()) {
                if (block.equals(first)) {
                    continue;
                }
                HashSet<BasicBlock> dom = new HashSet<>(function.getBasicBlocks());
                for (BasicBlock pre: block.getPrecursor()) {
                    dom.retainAll(pre.getDom());
                }
                dom.add(block);
                if (!dom.equals(block.getDom())) {
                    changed = true;
                    block.setDom(dom);
                }
            }
        } while (changed);
        System.out.println(function.getName() + ":");
        for (BasicBlock basicBlock: function.getBasicBlocks()) {
            System.out.println("block_" + basicBlock.getName() + ": ");
            for (BasicBlock block: basicBlock.getDom()) {
                System.out.println(block.getName());
            }
        }
        System.out.println("\n");
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

    public Module getModule() {
        return module;
    }
}
