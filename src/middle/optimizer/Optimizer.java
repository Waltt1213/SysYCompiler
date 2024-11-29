package middle.optimizer;

import llvmir.Module;
import llvmir.values.BasicBlock;
import llvmir.values.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class Optimizer {
    private Module module;
    private ArrayList<Function> functions;
    private final Mem2reg mem2reg;
    private final DCE dce;
    private final RemovePhi removePhi;
    private final LiveAnalyze activeAnalyze;

    public Optimizer(Module module) {
        this.module = module;
        functions = module.getFunctions();
        mem2reg = new Mem2reg(module);
        dce = new DCE(module);
        removePhi = new RemovePhi(module);
        activeAnalyze = new LiveAnalyze(module.getFunctions());
    }

    public void optimizeSSA() {
        removeDeadBlocks(); // 移除死代码块
        buildDom();         // 计算支配关系
        calDF();            // 计算支配边界
        mem2reg.buildSSA(); // 实现SSA
        dce.dce();  // 删除死代码
    }

    public void optimizeBackend() {
        removePhi.removePhi();
        activeAnalyze.analyzeActiveVar();
    }

    // 删除死代码块
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
                HashSet<BasicBlock> initDom = new HashSet<>();
                initDom.add(block);
                block.setDom(initDom);
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
    }

    private void calDF() {
        for (Function function: functions) {
            calFuncDF(function);
        }
    }

    private void calFuncDF(Function function) {
        // 计算直接支配
        for (BasicBlock block: function.getBasicBlocks()) {
            block.calDirectDom();
        }
        for (BasicBlock b: function.getBasicBlocks()) {
            for (BasicBlock p: b.getPrecursor()) {
                BasicBlock a = p;
                while (!b.isStrictDomBy(a)) {
                    if (a.getDirectDom() != null) {
                        HashSet<BasicBlock> DF = a.getDF();
                        DF.add(b);
                        a = a.getDirectDom();
                    } else {
                        break;
                    }
                }
            }
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

    public Module getModule() {
        return module;
    }
}
