package middle.optimizer;

import llvmir.Module;
import llvmir.values.BasicBlock;
import llvmir.values.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class CFG {
    private final Module module;

    public CFG(Module module) {
        this.module = module;
    }

    public void buildCFG() {
        buildDom();         // 计算支配关系
        calDF();            // 计算支配边界
    }

    private void buildDom() {
        for (Function function: module.getFunctions()) {
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
        for (Function function: module.getFunctions()) {
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
}
