package backend;

import backend.mips.MipsDataSeg;
import backend.mips.MipsInstruction;
import llvmir.Module;
import llvmir.ValueType;
import llvmir.values.GlobalVariable;

import java.util.ArrayList;
import java.util.LinkedList;

public class Translator {
    private final Module module;
    private ArrayList<MipsDataSeg> dataSegment;
    private ArrayList<MipsInstruction> textSegment;

    public Translator(Module module) {
        this.module = module;
        dataSegment = new ArrayList<>();
        textSegment = new ArrayList<>();
    }

    public void genMipsCode() {
        genDataSegment();
        genTextSegment();
    }

    public void genDataSegment() {
        LinkedList<GlobalVariable> globalVariables = module.getGlobalValues();
        for (GlobalVariable globalVar: globalVariables) {
            String name = globalVar.getName();
            String dataType = getDataType(globalVar.getTp().getDataType(), globalVar.isString());
            String init = globalVar.getInitValtoString();
            MipsDataSeg data = new MipsDataSeg(name, dataType, init);
            dataSegment.add(data);
        }
    }

    public void genTextSegment() {

    }

    public ArrayList<MipsDataSeg> getDataSegment() {
        return dataSegment;
    }

    public ArrayList<MipsInstruction> getTextSegment() {
        return textSegment;
    }

    public String getDataType(ValueType.DataType dataType, boolean isString) {
        if (isString) {
            return ".asciiz";
        }
        if (dataType == ValueType.DataType.Integer32Ty) {
            return ".word";
        }
        return ".byte";
    }
}
