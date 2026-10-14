package ce.ajneb97.variables;

import java.util.ArrayList;
import java.util.List;

public class Variable {
    private String name;
    private VariableType type;
    private ValueType valueType;
    private String initialValue;
    private List<String> possibleValues;
    private double minValue = -Double.MAX_VALUE;
    private double maxValue = Double.MAX_VALUE;
    private int maxCharacters = 256;
    private int maxDecimals = 2;
    private boolean manageOutOfRange = false;

    public Variable(String name, VariableType type, ValueType valueType, String initialValue) {
        this.name = name;
        this.type = type;
        this.valueType = valueType;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.possibleValues = new ArrayList<>();
    }

    public String getName() { return name; }
    public VariableType getType() { return type; }
    public ValueType getValueType() { return valueType; }
    public String getInitialValue() { return initialValue; }
    public void setInitialValue(String initialValue) { this.initialValue = initialValue; }
    public List<String> getPossibleValues() { return possibleValues; }
    public void setPossibleValues(List<String> possibleValues) { this.possibleValues = possibleValues; }
    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }
    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
    public int getMaxCharacters() { return maxCharacters; }
    public void setMaxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; }
    public int getMaxDecimals() { return maxDecimals; }
    public void setMaxDecimals(int maxDecimals) { this.maxDecimals = maxDecimals; }
    public boolean isManageOutOfRange() { return manageOutOfRange; }
    public void setManageOutOfRange(boolean manageOutOfRange) { this.manageOutOfRange = manageOutOfRange; }

    public boolean isNumerical() {
        return valueType == ValueType.INTEGER || valueType == ValueType.DOUBLE;
    }

    public List<String> getPossibleRealValues() {
        List<String> result = new ArrayList<>();
        for(String pv : possibleValues) {
            String[] split = pv.split(";", 2);
            result.add(split[0]);
        }
        return result;
    }

    public String getDisplayFromValue(String value) {
        for(String pv : possibleValues) {
            String[] split = pv.split(";", 2);
            if(split[0].equals(value)) {
                return split.length > 1 ? split[1] : "noDisplay";
            }
        }
        return null;
    }
}