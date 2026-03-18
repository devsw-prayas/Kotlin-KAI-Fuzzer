package io.kai.contracts;

public record Parameter(String name, String type, String defaultVal, boolean isVarargs, boolean isCrossInline, boolean
                        isNoInline) {
    public static Parameter simple(String name, String type){
        return new Parameter(name, type, "null", false, false , false);
    }
}
