# Kai — MVP-2: Full Mutation Engine

MVP-2 upgrades every layer of the system built in MVP-1. The expression model, builder capabilities, scope awareness, mutation scheduler, and mutation policy set are all new. The result is a fuzzer that generates structurally complex, semantically valid Kotlin programs at AST level and explores program-space via MCMC-style mutation chains with spatial repulsion.

---

## What Changed From MVP-1

| Layer | MVP-1 | MVP-2 |
|---|---|---|
| Mutations | 6 basic policies | 24 targeted policies across 4 tiers |
| Builders | 6 types | 24 types — full expression hierarchy |
| Scheduler | `RandomScheduler` | `CentroidWeightedScheduler` with Spatial Bidirectional Jitter |
| Scope | None | `ScopeContext`, `SymbolTable`, `TypeScope`, `ValueScope` |
| Minimizer | `NoOpMinimizer` | `DeltaMinimizer` — iterative child removal |
| Seeds | 1 shape | 3 structural shapes, randomly selected |
| Expression system | Single `ExpressionBuilder` | 11 dedicated expression builders |
| Deduplication | None | `FindingDeduplicator` — stack trace signature hashing |

---

## Builder Tree

The program tree now covers the full range of Kotlin constructs. Mutations operate exclusively on this tree — never on emitted source.

```
ProgramBuilder
├── ClassBuilder              (sealed, data, abstract, open, object, generic)
│   ├── FunctionBuilder       (inline, suspend, operator, generic, reified)
│   │   ├── VariableBuilder   (val/var, nullable, lateinit)
│   │   ├── BranchBuilder     (if/else)
│   │   ├── LoopBuilder       (while/for)
│   │   ├── TryCatchBuilder   (configurable exception type)
│   │   ├── WhenBuilder       (exhaustive when with typed branches)
│   │   ├── LambdaBuilder     (typed params, body)
│   │   └── RawStatementBuilder
│   ├── ObjectBuilder         (companion object / standalone)
│   └── ExtensionFunctionBuilder
├── SealedClassBuilder        (with nested subclasses)
├── TypeAliasBuilder
└── FunctionBuilder           (top-level)

Expression builders:
  IntLiteralBuilder, StringLiteralBuilder, BoolLiteralBuilder, NullLiteralBuilder,
  BinaryOpBuilder, UnaryOpBuilder, VariableRefBuilder, FunctionCallBuilder,
  SafeCallBuilder, ElvisBuilder, LambdaBuilder
```

---

## Mutation Policies — All 24

### Tier 1 — Reified / Inline (Nastiness 0.10–0.15)

| Policy | Nastiness | Emits |
|---|---|---|
| `AddReifiedInlineMutation` | 0.10 | `inline fun <reified T> fun_N(): Unit` |
| `AddCrossinlineMutation` | 0.12 | `inline fun foo(crossinline block: () -> Unit)` |
| `AddNoinlineMutation` | 0.15 | `inline fun foo(noinline block: () -> Unit)` |
| `AddReifiedClassCheckMutation` | 0.13 | `val x: Class<T> = T::class.java` inside reified context |
| `AddReifiedNewInstanceMutation` | 0.11 | `T::class.java.newInstance()` inside reified context |

### Tier 2 — Recursive / Deep Generics (Nastiness 0.15–0.22)

| Policy | Nastiness | Emits |
|---|---|---|
| `AddRecursiveGenericBoundMutation` | 0.15 | `class Foo<T : Foo<T>>` |
| `AddDeepGenericNestingMutation` | 0.18 | `Map<List<Set<T>>, T>?` — 1–3 levels deep |
| `AddSelfReferentialTypeAliasMutation` | 0.13 | `typealias Alias = List<Alias>` |
| `AddContravariantBoundMutation` | 0.20 | `<T : Comparable<T>>` |
| `AddMultipleUpperBoundMutation` | 0.22 | `<T> where T : Runnable, T : Serializable` |

### Tier 3 — Coroutines / Sealed / Operator / Lambda (Nastiness 0.25–0.55)

| Policy | Nastiness | Emits |
|---|---|---|
| `AddSuspendFunctionMutation` | 0.25 | `suspend` modifier on function |
| `AddSealedClassMutation` | 0.28 | `sealed class` with 2–3 subclasses |
| `WrapInTryCatchMutation` | 0.35 | Wraps body in `try { } catch (e: ExceptionType) { }` |
| `AddTypeAliasMutation` | 0.35 | `typealias MyList<T> = List<T>` |
| `AddWhenOnSealedMutation` | 0.30 | Exhaustive `when` on sealed type |
| `AddLambdaMutation` | 0.50 | `val var_N: () -> ReturnType = { body }` |
| `AddOperatorOverloadMutation` | 0.45 | `operator fun plus/minus/times/div/rem/compareTo/unaryMinus/not()` |
| `AddCompanionObjectMutation` | 0.55 | `companion object { fun fun_N(): Unit { } }` |

### MVP-1 Originals (Nastiness 0.60–0.80)

| Policy | Nastiness | Emits |
|---|---|---|
| `AddVariableMutation` | 0.80 | `val var_N: Type = expr` |
| `AddLoopMutation` | 0.70 | `while` / `for` loop |
| `InjectNullCheckMutation` | 0.65 | `val var_N: Type? = null` |
| `ExpandExpressionMutation` | 0.75 | Wraps expression in binary operation |
| `GenericMutation` | 0.60 | Adds type parameter `T_N` |
| `AddFunctionMutation` | 0.72 | Adds new `FunctionBuilder` child |

---

## CentroidWeightedScheduler — Spatial Bidirectional Jitter

The production scheduler. Prevents clustering by maintaining a 2D mutation centroid in `(depth, siblingIndex)` space. Nodes far from the centroid receive a repulsion boost; nodes near it are penalised.

```
weight = max(0.01, interestScore + depthJitter + repulsion)

repulsion = distance(node, centroid) × REPULSION_STRENGTH   (0.4)
centroid  = EMA of recently mutated node positions           (α = 0.3)
warmup    = 3 steps before repulsion activates
```

Policy selection targets a nastiness score derived from the node's effective interest — high-interest nodes attract nastier mutations.

### Builder Interest Scores (selected)

| Builder Configuration | Score |
|---|---|
| `FunctionBuilder` — inline + reified | 0.95 |
| `FunctionBuilder` — suspend + inline + reified | 0.93 |
| `FunctionBuilder` — suspend + generic | 0.88 |
| `ClassBuilder` — sealed + generic | 0.88 |
| `FunctionBuilder` — inline + generic | 0.85 |
| `ExtensionFunctionBuilder` | 0.72 |
| `LambdaBuilder` | 0.68 |
| `FunctionBuilder` — plain | 0.55 |

---

## Scope Context System

Scope-aware mutations query `ScopeContext` to emit semantically valid references — variables, types, and functions that are actually in scope at the point of injection.

| Component | Responsibility |
|---|---|
| `ScopeContext` | Hierarchical scope — each class/function introduces a new level |
| `TypeScope` | Type parameters declared at this scope level |
| `ValueScope` | Variables declared at this scope level |
| `SymbolTable` | All declared functions and classes globally |
| `ScopeContextBuilder` | Walks the builder tree, builds `ScopeContext` at each node |

---

## Seed Shapes

Three structural shapes, randomly selected on each `next()` call:

| Shape | Structure | Why |
|---|---|---|
| Basic class | `class { fun { var } }` | Original baseline |
| Inline reified | `class { inline fun <reified T> { T::class.java } }` | Highest-interest node from iteration 1 |
| Generic + suspend inline reified | `class<T : Comparable<T>> { suspend inline fun <reified T> { T::class.java, listOf<T>() } }` | Richest single-class seed |

---

## Infrastructure Upgrades

| Component | Change |
|---|---|
| `DeltaMinimizer` | Iterative child removal — recompiles after each removal, keeps pruned tree if oracle still fires |
| `FindingDeduplicator` | Stack trace signature hashing — first 3 kotlinc frames, prevents artifact explosion on repeated ICEs |
| `ArtifactStore` | Saves `program.kt` + `stderr.log` + `chain.json` + `verdict.json` per finding |
| `FuzzerStats` | `iter/s`, corpus size, findings — daemon reporter thread, every 10 seconds |
| `NameRegistry` | Atomic counters per node type — `next("fun")` → `fun_0`, `fun_1`, ... Shared per seed, snapshot for chain replay |

---

## Run

```bash
java -jar build/libs/kai-mvp-1.0-SNAPSHOT.jar \
  -kotlinc /path/to/kotlinc \
  -log ./crashes \
  -smt 4
```

### CLI Flags

| Flag | Default | Description |
|---|---|---|
| `-kotlinc` | required | Path to `kotlinc` binary |
| `-log` | `./logs` | Output directory for crash artifacts |
| `-mdepth` | `5` | Max mutation chain depth |
| `-smt` | `1` | Thread count |
| `-timeout` | `30000` | Per-compilation timeout in ms |
| `-b` | `50` | Batch size per thread |
| `-maxiter` | `0` | Max iterations (0 = unlimited) |

---

## Project Structure

```
src/main/java/io/kai/
├── artifact/        ArtifactStore, FindingDeduplicator
├── builders/        13 tree builders + expressions/ (11 expression builders)
├── compiler/        CompilerRunner, IOracle, OracleVerdict, oracles/, coverage/
├── contracts/       IBuilder, NameRegistry, Parameter, capability/ (8 interfaces)
├── corpus/          ICorpusManager, SimpleCorpusManager, CorpusMeta
├── fuzzer/          FuzzerEngine, FuzzerContext, FuzzerConfig, FuzzerRuntime, FuzzerStats
├── llm/             ILLMProvider, NoOpLLMProvider
├── minimize/        IMinimizer, DeltaMinimizer, NoOpMinimizer
├── mutation/        IMutationPolicy, MutationRegistry, chain/, context/, mutators/
├── scheduler/       IScheduler, CentroidWeightedScheduler, RandomScheduler
└── seed/            ISeedProvider, SyntheticSeedProvider
```

---

## Status

**v0.2.0-mvp2** — Full mutation engine operational. 24 policies, scope-aware mutations, spatial jitter scheduler, delta minimizer, deduplication. Generating 500+ LOC programs in under 10 minutes, all compiling clean on K2 2.3.
