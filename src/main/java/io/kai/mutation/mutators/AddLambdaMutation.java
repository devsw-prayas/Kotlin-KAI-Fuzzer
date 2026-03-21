package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.BoolLiteralBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.builders.expressions.StringLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.ScopeContext;

import java.util.List;
import java.util.Set;

public class AddLambdaMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_lambda"; }
    @Override
    public boolean compatibleWith(IBuilder b) {
        if (!(b instanceof FunctionBuilder)) return false;
        // Cap at 2 lambdas per function
        long lambdaCount = b.children().stream()
                .filter(c -> c instanceof VariableBuilder vb
                        && vb.getType().startsWith("() ->"))
                .count();
        return lambdaCount < 2;
    }
    private static final String[] SAFE_RETURN_TYPES = {"Unit", "Int", "Boolean", "String"};

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        String returnType = SAFE_RETURN_TYPES[ctx.rng().nextInt(SAFE_RETURN_TYPES.length)];
        LambdaBuilder lambda = getLambdaBuilder(builder, returnType);
        String lambdaType = "() -> " + returnType;
        VariableBuilder var = new VariableBuilder(
                builder.getRegistry(), false, lambda, false, lambdaType);
        if (builder instanceof IContainer<?> c) MutationUtility.addChildSmart(c, var);
        return builder;
    }

    private static LambdaBuilder getLambdaBuilder(IBuilder builder, String returnType) {
        LambdaBuilder lambda = new LambdaBuilder(builder.getRegistry(), List.of(), returnType);

        // Add a body expression matching the return type
        if (!returnType.equals("Unit")) {
            ILocalScopeBuilder literalExpr = switch (returnType) {
                case "Boolean" -> new BoolLiteralBuilder(builder.getRegistry(), "false");
                case "String" -> new StringLiteralBuilder(builder.getRegistry(), "\"\"");
                default -> new IntLiteralBuilder(builder.getRegistry(), "0");
            };
            lambda.addChild(literalExpr);
        }
        return lambda;
    }
}