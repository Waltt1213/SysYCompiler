package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.Function;

import java.util.ArrayList;

public class Call extends Instruction {
    private Function callFunc;
    private ArrayList<Value> funcRParams;

    public Call(String name, Function callFunc) {
        super(callFunc.getTp(), Type.CALL, name);
        this.callFunc = callFunc;
        funcRParams = new ArrayList<>();
    }

    public Call(Function callFunc) {
        super(callFunc.getTp(), Type.CALL, null);
        this.callFunc = callFunc;
        funcRParams = new ArrayList<>();
    }

    public void setName(String name) {
        super.setName(name);
    }

    public void setFuncRParams(ArrayList<Value> funcRParams) {
        this.funcRParams = funcRParams;
    }

    public void addFuncRParam(Value param) {
        funcRParams.add(param);
    }

    public boolean isNotVoid() {
        return callFunc.isNotVoid();
    }

    public Function getCallFunc() {
        return callFunc;
    }

    public ArrayList<Value> getFuncRParams() {
        return funcRParams;
    }

    public String getCall() {
        StringBuilder sb = new StringBuilder();
        sb.append(callFunc.getTp().toString()).append(" ");
        sb.append(callFunc.getFullName()).append("(");
        for (int i = 0; i < funcRParams.size(); i++) {
            sb.append(funcRParams.get(i).getDef());
            if (i < funcRParams.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        if (tp.getDataType().equals(ValueType.DataType.VoidTy)) {
            return "call " + getCall();
        } else {
            return getFullName() + " = call " + getCall();
        }
    }
}
