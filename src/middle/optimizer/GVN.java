package middle.optimizer;

import llvmir.Module;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.instr.BinaryOperator;
import llvmir.values.instr.Compare;
import llvmir.values.instr.GetElementPtr;
import llvmir.values.instr.Instruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class GVN {
    private Module module;
    private HashMap<String, Instruction> gvnHashMap = new HashMap<>();

    public GVN(Module module) {
        this.module = module;
    }

    public void gvn() {
        module.setVirtualName();
        for (Function function: module.getFunctions()) {
            gvnForBlock(function.getBasicBlocks().get(0));
        }
    }

    public void gvnForBlock(BasicBlock block) {
        LinkedList<Instruction> instructions = new LinkedList<>(block.getInstructions());
        HashSet<Instruction> hashSet = new HashSet<>();
        for (Instruction instr: instructions) {
            String hash = genHash(instr);
            if (!hash.isEmpty()) {
                if (gvnHashMap.containsKey(hash)) {
                    instr.replaceAllUses(gvnHashMap.get(hash));
                    instr.remove();
                } else {
                    gvnHashMap.put(hash, instr);
                    hashSet.add(instr);
                }
            }
        }
        for (BasicBlock basicBlock: block.getDomChild()) {
            gvnForBlock(basicBlock);
        }
        for (Instruction instruction: hashSet) {
            gvnHashMap.remove(genHash(instruction));
        }
    }

    private String genHash(Instruction instruction) {
        if (instruction instanceof BinaryOperator) {
            BinaryOperator binaryOperator = (BinaryOperator) instruction;
            Value operand1 = binaryOperator.getOperands().get(0);
            Value operand2 = binaryOperator.getOperands().get(1);
            if (binaryOperator.getIrType().equals(Instruction.Type.ADD)
                    || binaryOperator.getIrType().equals(Instruction.Type.MUL)) {
                if (operand1.getFullName().compareTo(operand2.getFullName()) > 0) {
                    return hash(binaryOperator.getIrType().toString(), operand1, operand2);
                } else {
                    return hash(binaryOperator.getIrType().toString(), operand2, operand1);
                }
            } else {
                return hash(binaryOperator.getIrType().toString(), operand1, operand2);
            }
        } else if (instruction instanceof Compare) {
            Compare compare = (Compare) instruction;
            Value operand1 = compare.getOperands().get(0);
            Value operand2 = compare.getOperands().get(1);
            if (compare.getCondType().equals(Compare.CondType.EQ)
                    || compare.getCondType().equals(Compare.CondType.NE)) {
                if (operand1.getFullName().compareTo(operand2.getFullName()) > 0) {
                    return hash(compare.getCondType().toString(), operand1, operand2);
                } else {
                    return hash(compare.getCondType().toString(), operand2, operand1);
                }
            } else {
                return hash(compare.getCondType().toString(), operand1, operand2);
            }
        } else if (instruction instanceof GetElementPtr) {
            StringBuilder sb = new StringBuilder();
            sb.append(Instruction.Type.GETPTR).append(" ");
            for (Value value: instruction.getOperands()) {
                sb.append(value.getFullName()).append(" ");
            }
            return sb.toString();
        }
        return "";
    }

    public String hash(String type, Value operand1, Value operand2) {
        return type + " " + operand1.getFullName() + " " + operand2.getFullName();
    }

}
