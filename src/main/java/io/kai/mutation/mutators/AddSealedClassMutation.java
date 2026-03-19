package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.ProgramBuilder;
import io.kai.builders.SealedClassBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds a sealed class with 2-3 subclasses
public class AddSealedClassMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ProgramBuilder.class); }
    @Override public String id() { return "add_sealed_class"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ProgramBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ProgramBuilder pb = (ProgramBuilder) builder;
        SealedClassBuilder sealed = new SealedClassBuilder(builder.getRegistry());
        int subCount = 2 + ctx.rng().nextInt(2); // 2 or 3 subclasses
        for (int i = 0; i < subCount; i++) {
            ClassBuilder sub = new ClassBuilder(builder.getRegistry());
            sealed.addSubclass(sub);
        }
        pb.addChild(sealed);
        return builder;
    }
}
