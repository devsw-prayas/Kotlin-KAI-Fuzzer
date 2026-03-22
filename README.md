# Kai — MVP-2 Part II: K2 Destabilization Pass

The destabilization pass is a second processing layer that runs in parallel on top of the construction pass. It takes valid programs from the corpus and injects targeted stress constructs targeting experimental and newly-stabilised features in kotlinc 2.2.20–2.3.x — code paths that are brand new, have never been fuzz-tested, and in several cases require compiler flags that did not exist before 2.3.

---

## Architecture

```
Construction Pass (workerLoop)          Destabilizer Pass (destabilizerLoop)
─────────────────────────────           ────────────────────────────────────
Grow corpus via mutation chains         30% probability per clean compile
↓                                       ↓
Valid programs only                     Pick corpus entry
↓                                       ↓
Corpus ──────────────────────────────► canApply() filter
                                        ↓
                                        destabilize() — ADD only, in-place
                                        ↓
                                        compile with globalFlags
                                        ↓
                                        oracle → dedup → minimize → artifact
```

The two passes are completely independent. Destabilizer-injected constructs become part of the corpus entry permanently. Construction mutations in subsequent iterations build on top of them — producing emergent combinations that neither pass could generate alone. This is intentional: the destabilized corpus is richer and pushes the MCMC chain into more complex regions of program-space.

---

## Global Flag Infrastructure

All flags required by any registered destabilizer are collected automatically at startup via `requiredFlags()` and passed to every `kotlinc` invocation — both the construction pass and the destabilizer pass. No manual flag passing required.

```
-Xexplicit-backing-fields
-Xallow-holdsin-contract
-Xallow-contracts-on-more-functions
-opt-in=kotlin.contracts.ExperimentalContracts
-opt-in=kotlin.contracts.ExperimentalExtendedContracts
-Xallow-reified-type-in-catch
-Xdata-flow-based-exhaustiveness
```

This means the corpus is never contaminated — programs that require experimental flags compile correctly in both passes because every compilation uses the same global flag set.

---

## New Builder Capabilities

The following builder upgrades were added to support the destabilizer pass:

| Upgrade | Purpose |
|---|---|
| `VariableBuilder.withExplicitBackingField(fieldType, fieldExpr)` | Emits `field = expr` — V1 attack vector |
| `VariableBuilder.withGetterBlock(String block)` | Emits `get() { block }` — V3b attack vector |
| `IFirstStatement` interface | Pinned first statement immune to construction mutation body wrapping. Implemented by `FunctionBuilder` and `ExtensionFunctionBuilder`. |
| `Supplier<String> firstStatement` | Lazy first statement — deferred evaluation at `build()` time |
| `ExtensionFunctionBuilder` implements `IGeneric` | Extension functions can declare their own type params |
| `Supplier<String> receiverType` | Lazy receiver type — resolved after construction mutations finalise referenced class type params |
| `RawStatementBuilder` | Universal injection mechanism for constructs no builder models directly |

---

## K2 Attack Vectors

Six experimental or actively-modified subsystems in kotlinc 2.2.20–2.3.x form the attack surface:

| ID | Subsystem | Flag | Why It's Fragile |
|---|---|---|---|
| **V1** | Explicit Backing Fields | `-Xexplicit-backing-fields` | New `field = expr` syntax. FIR tracks two types per property — declared and field type. Smart casting through the field type layer is new code. |
| **V2** | Reified Catch Clauses | `-Xallow-reified-type-in-catch` | Reified substitution in a catch clause is new in 2.2.20. The inliner must materialise the reified type into the JVM exception table — a position it has never operated in before. Interaction with coroutine state machine generation is completely untested. |
| **V3b** | Contracts in Accessors | `-Xallow-contracts-on-more-functions` | Contract declarations inside property getters are new. The getter contract must be resolved before the property type is finalised — potential FIR phase ordering issue. |
| **V3c** | `holdsIn` Keyword | `-Xallow-holdsin-contract` | The least tested feature in 2.3. Propagates a condition assumption into a lambda body — an entirely new form of contract interacting with lambda capture analysis and smart casting. |
| **V4** | Context-Sensitive Resolution | none (default, actively changing) | Sealed and enclosing supertype scopes now included in contextual scope as of 2.3. Actively being modified — moving code is fragile code. |
| **V5** | Data-Flow Exhaustiveness | `-Xdata-flow-based-exhaustiveness` | New DFA analysis pass feeds the exhaustiveness checker. DFA tracks early returns and condition checks across lambda boundaries and generic type params — both new interactions. |

---

## The 9 Destabilizers

### Tier A — Single Vector

#### DA-3 — `ExplicitBackingFieldSuspendDestabilizer` · V1
Injects a property with explicit backing field whose field type is `suspend () -> Unit` — a suspend function type in the backing field position has never been seen by FIR before.

```kotlin
val var_N: Any
    field = suspend { }
```

**Target:** `ClassBuilder`. **Guard:** `build(0).contains("field = suspend {")`. **Flag:** `-Xexplicit-backing-fields`

---

#### DA-8 — `ContractHoldsInDestabilizer` · V3c
Injects a `holdsIn` contract into an inline function via `IFirstStatement` — guaranteed position 0 regardless of what construction mutations do to the body. Targets non-operator inline functions without an existing first statement.

```kotlin
@OptIn(kotlin.contracts.ExperimentalContracts::class,
       kotlin.contracts.ExperimentalExtendedContracts::class)
inline fun <...> fun_N(cond_N: Boolean, noinline block_N: () -> Unit): Unit {
    kotlin.contracts.contract { cond_N.holdsIn<Unit>(block_N) }
    block_N()
    // ... existing body ...
}
```

**Target:** `FunctionBuilder` where `isInline && !isOperator && !hasFirstStatement()`. **Flags:** `-Xallow-holdsin-contract`, `-opt-in=...ExperimentalContracts`, `-opt-in=...ExperimentalExtendedContracts`

---

### Tier B — Two-Vector Cross Hybrids

#### DB-1 — `ExplicitBackingFieldContractDestabilizer` · V1 × V3b
Explicit backing field + contract getter on the same class. FIR must track two types per property AND resolve a contract implication simultaneously.

```kotlin
val var_N: Any?
    field = null as T_0?
val var_M: Boolean
    get() {
        kotlin.contracts.contract { returns(true) implies (this@class_N is class_N) }
        return true
    }
```

**Target:** `ClassBuilder`. **Flags:** `-Xexplicit-backing-fields`, `-Xallow-contracts-on-more-functions`, `-opt-in=...ExperimentalContracts`

---

#### DB-2 — `ReifiedCatchSuspendDestabilizer` · V2 × coroutine
Reified catch inside a `suspend inline` function — the coroutine state machine generator and the new exception table handler have never had to cooperate.

```kotlin
suspend inline fun <..., reified T_N : Throwable, ...> fun_N(...): Unit {
    try { block_N() } catch (e: T_N) {
        val var_N: Class<T_N> = T_N::class.java
    }
}
```

**Target:** `FunctionBuilder` where `isInline && isSuspend`. **Flag:** `-Xallow-reified-type-in-catch`

---

#### DB-3 — `ReifiedCatchStormDestabilizer` · V2 × reified storm
All three reified operations stacked on the same type param, plus a reified catch clause — maximum reified substitution work on the new catch code path. Uses `run { val _any: Any = 42; _any is T }` to avoid always-false static analysis warnings.

```kotlin
val var_N: Class<T_N> = T_N::class.java
val var_M: Boolean = run { val _any_T_N: Any = 42; _any_T_N is T_N }
val var_P: List<T_N> = listOf<T_N>()
try { } catch (e: T_N) { val _cls = T_N::class.java }
```

**Target:** `FunctionBuilder` where `isInline`. **Flag:** `-Xallow-reified-type-in-catch`

---

#### DB-4 — `DataFlowExhaustivenessReifiedDestabilizer` · V5 × V2
DFA exhaustiveness where the when subject is a sealed type — DFA must track return paths through a type the compiler is simultaneously substituting. Targets `Unit` functions only. Uses qualified subclass names (`sealed_N.class_N`).

```kotlin
if (x_N is sealed_N.class_1) return
when (x_N) {
    is sealed_N.class_2 -> { Unit }
    is sealed_N.class_3 -> { Unit }
}
// No else branch — DFA must prove sub0 eliminated by early return
```

**Target:** `FunctionBuilder` where `!isOperator && returnType == "Unit"` and no existing `WhenBuilder`. **Flag:** `-Xdata-flow-based-exhaustiveness`

---

#### DB-5 — `ContextSensitiveContractDestabilizer` · V4 × V3a
Contract generic type assertion where the asserted type is context-sensitively resolved through the sealed hierarchy. Extension function receiver type is computed lazily via `Supplier<String>` — immune to construction mutations adding type params to the result class after injection. Sentinel typealias used as injection marker.

```kotlin
@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun <T_N : sealed_0> class_N<T_N>.ext_fun_N(): Boolean {
    kotlin.contracts.contract {
        returns(true) implies (this@ext_fun_N is class_N<sealed_0.class_1>)
    }
    return this.value is sealed_0.class_1
}
typealias DestabSentinel_sealed_0 = Boolean
```

**Target:** `ProgramBuilder` with `SealedClassBuilder` having 2+ subclasses. **Guard:** `DestabSentinel_` typealias. **Flags:** `-Xallow-contracts-on-more-functions`, `-opt-in=...ExperimentalContracts`

---

### Tier C — Multi-Vector

#### DC-1 — `ExplicitBackingFieldReifiedCatchDestabilizer` · V1 × V2 × V3b
Three new 2.3 subsystems on one class — explicit backing field, contract getter, and reified catch inline function all referencing the same `Throwable` type.

```kotlin
val var_N: Throwable                         // V1
    field = RuntimeException()
val var_M: Boolean                           // V3b
    get() {
        kotlin.contracts.contract {
            returns(true) implies (this@class_N is class_N)
        }
        return var_N is RuntimeException
    }
inline fun <reified T_N : Throwable> fun_N(block_N: () -> Unit): Unit {  // V2
    try { block_N() } catch (e: T_N) {
        val var_P: Class<T_N> = T_N::class.java
    }
}
```

**Target:** `ClassBuilder` where `!isObject`. **Guard:** `build(0).contains("field = RuntimeException()")`. **Flags:** `-Xexplicit-backing-fields`, `-Xallow-reified-type-in-catch`, `-Xallow-contracts-on-more-functions`, `-opt-in=...ExperimentalContracts`

---

#### DC-2 — `DataFlowContractExhaustivenessDestabilizer` · V5 × V3a × V4
Contract establishes early return condition → DFA uses it to eliminate else branch → context-sensitive resolution walks the sealed hierarchy. Three analysis passes coupled in a pipeline that did not exist before 2.3. Sentinel typealias `DestabDFSentinel_` used as injection marker.

```kotlin
@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun <T_N : sealed_0> class_N<T_N>.ext_fun_N(shape_N: T_N): String {
    kotlin.contracts.contract {
        returns() implies (shape_N !is sealed_0.class_1)
    }
    if (shape_N is sealed_0.class_1) return "sealed_0.class_1"
    return when (shape_N) {
        is sealed_0.class_2 -> "sealed_0.class_2"
        is sealed_0.class_3 -> "sealed_0.class_3"
    }
}
typealias DestabDFSentinel_sealed_0 = Boolean
```

**Target:** `ProgramBuilder` with `SealedClassBuilder` having 2+ subclasses. **Guard:** `DestabDFSentinel_` typealias. **Flags:** `-Xdata-flow-based-exhaustiveness`, `-Xallow-contracts-on-more-functions`, `-opt-in=...ExperimentalContracts`

---

## Destabilizer Invariants

- **ADD only** — destabilizers never remove, rearrange, or replace existing nodes
- **`canApply()` is side-effect free** — no tree mutations in the eligibility check
- **Guard strings prevent double-injection** — each destabilizer checks for its own injection marker before firing
- **`IFirstStatement` guarantees position 0** — contract blocks are always the first statement in a function body, immune to construction mutations wrapping the body in loops or try/catch
- **Lazy `Supplier<String>` receivers** — extension function receiver types are computed at `build()` time, after all construction mutations have finalised the referenced class's type params
- **Destabilizer constructs persist in the corpus** — injections are permanent. Subsequent construction mutations build on destabilized programs, producing compound structures that emerge organically from the interaction of both passes. globalFlags ensures all corpus entries compile under the same flag set regardless of which constructs were injected.

---

## Emergent Combinations

Because destabilizers fire on programs already mutated by the construction pass, the combinations produced exceed what any individual destabilizer generates alone:

- **DA-3 + DB-1 + DC-1** firing on the same class — multiple backing fields and contract getters simultaneously
- **DA-8** on a function already made `suspend inline reified` by construction — `holdsIn` contract on a suspend inline reified function, not present in any single destabilizer design
- **Reified backing field check** — `val var_N: Boolean = run { val _any: Any = (var_1); _any is T_5 }` — the crawler used an explicit backing field property as the subject of a reified is-check
- **Contract getter in lambda** — `val var_N: () -> Boolean = { (false||var_3) }` — a Boolean contract getter property captured in a lambda expression

---

## Run

```bash
java -jar build/libs/kai-mvp-1.0-SNAPSHOT.jar \
  -kotlinc /path/to/kotlinc \
  -log ./crashes \
  -destab true
```

### CLI Flags

| Flag | Default | Description |
|---|---|---|
| `-kotlinc` | required | Path to `kotlinc` binary |
| `-log` | `./logs` | Output directory for crash artifacts |
| `-destab` | `true` | Enable destabilization pass |
| `-smt` | `1` | Thread count |
| `-timeout` | `30000` | Per-compilation timeout in ms |
| `-maxiter` | `0` | Max iterations (0 = unlimited) |

---

## Status

**v0.2.0-mvp2-destab** — 9 K2 destabilizers operational across 3 tiers. Covers 6 attack vectors targeting experimental features in kotlinc 2.2.20–2.3.x. Already triggering open compiler warnings (KTLC-365) in early runs. All destabilizers compile clean on K2 2.3 independently and in combination.
