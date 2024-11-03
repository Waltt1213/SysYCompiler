package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;

import java.util.ArrayList;

public class Label extends Value {
    private ArrayList<Label> preds;

    public Label(String name) {
        super(new ValueType.Type(ValueType.DataType.LabelTy), name);
        preds = new ArrayList<>();
    }

    public void addPred(Label label) {
        preds.add(label);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(": \n");
        sb.append("; preds = ");
        for (Label label: preds) {
            sb.append(label.getFullName());
            sb.append(" ");
        }
        sb.append("\n");
        return sb.toString();
    }
}
