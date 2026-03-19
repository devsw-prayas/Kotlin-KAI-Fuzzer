package io.kai.seed;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

public class SyntheticSeedProvider implements ISeedProvider{
    private NameRegistry registry;
    @Override
    public NameRegistry getRegistry() {
        return registry;
    }

    public SyntheticSeedProvider(){
        registry = new NameRegistry();
    }

    @Override
    public IBuilder next() {
        registry = new NameRegistry();
        ProgramBuilder builder = new ProgramBuilder(registry);
        ClassBuilder myClass = new ClassBuilder(registry);
        FunctionBuilder myFunc = new FunctionBuilder(registry);
        IExpressionBuilder myExp = new IntLiteralBuilder(registry, "100");
        VariableBuilder myVar = new VariableBuilder(registry, true, myExp, false);
        myFunc.addChild(myVar);
        myClass.addChild(myFunc);
        builder.addChild(myClass);
        return builder;
    }

    @Override
    public boolean hasMore() {
        return true;
    }

    @Override
    public void reset() {

    }
}
