package io.kai.contracts;

public record BuildContext(int indentLevel, NameRegistry nameRegistry, TypeScope typeScope) {
    public static BuildContext defaultContext(){
        return new BuildContext(0, new NameRegistry(), new TypeScope());
    }
}