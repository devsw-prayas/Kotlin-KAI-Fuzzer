package io.kai.seed;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;

import java.util.List;
import java.util.Random;

public class SyntheticSeedProvider implements ISeedProvider {
    private NameRegistry registry;
    private final Random rng = new Random();

    @Override
    public NameRegistry getRegistry() { return registry; }

    public SyntheticSeedProvider() { registry = new NameRegistry(); }

    @Override
    public IBuilder next() {
        registry = new NameRegistry();
        return switch (rng.nextInt(6)) {
            case 1 -> seedInlineReified();
            case 3 -> seedGenericClassInlineFunc();
            case 4 -> seedSuspendClass();
            case 5 -> seedMultiClass();
            default -> seedBasicClass();
        };
    }

    // Shape 0 — class { fun { var } } — original
    private IBuilder seedBasicClass() {
        ProgramBuilder prog = new ProgramBuilder(registry);
        ClassBuilder cls = new ClassBuilder(registry);
        FunctionBuilder fn = new FunctionBuilder(registry);
        fn.addChild(new VariableBuilder(registry, true,
                new IntLiteralBuilder(registry, "100"), false, "Int"));
        cls.addChild(fn);
        prog.addChild(cls);
        return prog;
    }

    // Shape 1 — inline reified function — highest-interest builder from day 1
    private IBuilder seedInlineReified() {
        ProgramBuilder prog = new ProgramBuilder(registry);
        ClassBuilder cls = new ClassBuilder(registry);
        FunctionBuilder fn = new FunctionBuilder(registry);
        fn.setInline(true);
        String T = registry.next("T");
        fn.addBoundedTypeParam(T, "reified");
        fn.addChild(new VariableBuilder(registry, false,
                new IntLiteralBuilder(registry, T + "::class.java"),
                false, "Class<" + T + ">"));
        cls.addChild(fn);
        prog.addChild(cls);
        return prog;
    }

    // Shape 2 — sealed class + when expression
    private IBuilder seedSealedWhen() {
        ProgramBuilder prog = new ProgramBuilder(registry);
        SealedClassBuilder sealed = new SealedClassBuilder(registry);
        ClassBuilder sub1 = new ClassBuilder(registry);
        ClassBuilder sub2 = new ClassBuilder(registry);
        sealed.addSubclass(sub1);
        sealed.addSubclass(sub2);
        prog.addChild(sealed);

        FunctionBuilder fn = new FunctionBuilder(registry);
        String paramName = registry.next("x");
        fn.addParam(Parameter.simple(paramName, sealed.id()));

        WhenBuilder when = new WhenBuilder(registry,
                new io.kai.builders.expressions.VariableRefBuilder(
                        registry, paramName, sealed.id()));

        when.addBranch(new WhenBuilder.WhenBranch(
                new IntLiteralBuilder(registry, "is " + sealed.id() + "." + sub1.id()),
                List.of(new RawStatementBuilder(registry, "Unit")),
                false));

        when.addBranch(new WhenBuilder.WhenBranch(
                new IntLiteralBuilder(registry, "is " + sealed.id() + "." + sub2.id()),
                List.of(new RawStatementBuilder(registry, "Unit")),
                false));

        fn.addChild(when);
        prog.addChild(fn);
        return prog;
    }
    // Shape 3 — generic class with suspend inline reified function
    private IBuilder seedGenericClassInlineFunc() {
        ProgramBuilder prog = new ProgramBuilder(registry);
        ClassBuilder cls = new ClassBuilder(registry);
        String CT = registry.next("T");
        cls.addBoundedTypeParam(CT, "Comparable<" + CT + ">");

        FunctionBuilder fn = new FunctionBuilder(registry);
        fn.setInline(true);
        fn.setSuspend(true);
        String FT = registry.next("T");
        fn.addBoundedTypeParam(FT, "reified");
        fn.addChild(new VariableBuilder(registry, false,
                new IntLiteralBuilder(registry, FT + "::class.java"),
                false, "Class<" + FT + ">"));
        fn.addChild(new VariableBuilder(registry, false,
                new IntLiteralBuilder(registry, "listOf<" + FT + ">()"),
                false, "List<" + FT + ">"));
        cls.addChild(fn);
        prog.addChild(cls);
        return prog;
    }

    // Shape 4 — suspend class with coroutine-ready functions
    private IBuilder seedSuspendClass() {
        ProgramBuilder prog = new ProgramBuilder(registry);
        ClassBuilder cls = new ClassBuilder(registry);

        FunctionBuilder suspFn = new FunctionBuilder(registry);
        suspFn.setSuspend(true);
        suspFn.addChild(new VariableBuilder(registry, false,
                new NullLiteralBuilder(registry), true, "String"));
        cls.addChild(suspFn);

        FunctionBuilder inlineFn = new FunctionBuilder(registry);
        inlineFn.setInline(true);
        String blockName = registry.next("block");
        inlineFn.addParam(new Parameter(blockName, "() -> Unit",
                null, false, true, false));
        cls.addChild(inlineFn);

        prog.addChild(cls);
        return prog;
    }

    // Shape 5 — two classes with inheritance and generic bound
    private IBuilder seedMultiClass() {
        ProgramBuilder prog = new ProgramBuilder(registry);

        ClassBuilder base = new ClassBuilder(registry);
        base.setOpen(true);
        String BT = registry.next("T");
        base.addBoundedTypeParam(BT, "");
        FunctionBuilder baseFn = new FunctionBuilder(registry);
        baseFn.addChild(new VariableBuilder(registry, false,
                new NullLiteralBuilder(registry), true, BT));
        base.addChild(baseFn);
        prog.addChild(base);

        ClassBuilder derived = new ClassBuilder(registry);
        derived.addSuperType(base.id() + "<String>");
        FunctionBuilder derivedFn = new FunctionBuilder(registry);
        derivedFn.setInline(true);
        String DT = registry.next("T");
        derivedFn.addBoundedTypeParam(DT, "reified");
        derived.addChild(derivedFn);
        prog.addChild(derived);

        return prog;
    }

    @Override
    public boolean hasMore() { return true; }

    @Override
    public void reset() { }
}