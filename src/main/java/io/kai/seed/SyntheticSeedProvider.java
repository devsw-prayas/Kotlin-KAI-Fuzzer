package io.kai.seed;

import io.kai.builders.*;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;

public class SyntheticSeedProvider implements ISeedProvider{

    @Override
    public IBuilder next() {
        NameRegistry registry = new NameRegistry();
        ProgramBuilder builder = new ProgramBuilder(registry);
        ClassBuilder myClass = new ClassBuilder(registry);
        FunctionBuilder myFunc = new FunctionBuilder(registry);
        ExpressionBuilder myExp = new ExpressionBuilder(registry, ExpressionBuilder.ExpressionType.INT_LITERAL, "300");
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
