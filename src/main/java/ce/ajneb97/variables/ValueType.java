package ce.ajneb97.variables;

public enum ValueType {
    TEXT, INTEGER, DOUBLE;

    public static boolean isValid(ValueType type, String value) {
        if(value == null) return false;
        switch(type) {
            case INTEGER:
                try { Long.parseLong(value); return true; } catch(Exception e) { return false; }
            case DOUBLE:
                try { Double.parseDouble(value); return true; } catch(Exception e) { return false; }
            case TEXT:
            default:
                return true;
        }
    }
}