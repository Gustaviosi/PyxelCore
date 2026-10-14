package ce.ajneb97.variables;

public class VariableResult {
    private boolean error;
    private String errorMessage;
    private String resultValue;

    private VariableResult(boolean error, String errorMessage, String resultValue) {
        this.error = error;
        this.errorMessage = errorMessage;
        this.resultValue = resultValue;
    }

    public static VariableResult ok(String value) {
        return new VariableResult(false, null, value);
    }

    public static VariableResult error(String message) {
        return new VariableResult(true, message, null);
    }

    public boolean isError() { return error; }
    public String getErrorMessage() { return errorMessage; }
    public String getResultValue() { return resultValue; }
}