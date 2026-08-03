# Kai

Kai is a structure-aware fuzzer for the Kotlin K2 compiler frontend (kotlinc 2.2.20–2.3.x). Instead of mutating source text, it builds valid Kotlin ASTs programmatically, mutates them, compiles the result with `kotlinc`, and runs oracles over the output to catch crashes and internal compiler errors (ICEs).

The fuzzer runs two passes in parallel:

- **Construction pass** — grows a corpus of valid programs from scratch by applying chained mutations (`mutation/`) to AST builders (`builders/`). Driven by worker threads in `FuzzerEngine`.
- **Destabilization pass** — a second layer that takes valid programs already in the corpus and injects targeted stress constructs at experimental or newly-stabilised K2 subsystems (explicit backing fields, reified catch clauses, contracts in accessors, the `holdsIn` contract keyword, context-sensitive resolution, data-flow exhaustiveness). Injected constructs are permanent and become fodder for further construction mutations, producing combinations neither pass would generate alone.

Both passes share one corpus and one global set of compiler flags (`FuzzerRuntime.collectAllFlags()`), collected automatically from every registered mutation and destabilizer, so every compile — from either pass — succeeds regardless of which experimental features are involved.

## Build

Requires JDK 24.

```bash
./gradlew build
```

This produces a shaded jar (all dependencies included) at `build/libs/Kai-mvp-1.0-SNAPSHOT.jar`.

## Run

```bash
java -jar build/libs/Kai-mvp-1.0-SNAPSHOT.jar \
  -kotlinc /path/to/kotlinc \
  -log ./crashes \
  -destab true
```

Findings (minimized reproducer + compiler output) are written to the `-log` directory whenever an oracle flags a compile as a crash or ICE.

### CLI Flags

| Flag | Default | Description |
|---|---|---|
| `-kotlinc` | required | Path to `kotlinc` binary |
| `-log` | `./logs` | Output directory for logs and artifacts |
| `-destab` | `true` | Enable the destabilization pass |
| `-smt` | `1` | Thread count |
| `-timeout` | `30000` | Per-compilation timeout in ms |
| `-maxiter` | `0` | Max iterations (0 = unlimited) |
| `-b` | `50` | Batch size per thread |
| `-mdepth` | `5` | Max mutation chain depth |
| `-v`, `--verbose` | `false` | Enable verbose debug output |

## Project layout

```
src/main/java/io/kai/
  builders/      Fluent AST-builder DSL (classes, functions, loops, when, try/catch, expressions, ...)
  mutation/      Mutation policies + chain building + scope tracking (construction pass)
  destabilizer/  Destabilizer implementations + runner (destabilization pass)
  compiler/      kotlinc invocation, oracles (ICE/crash detection), coverage collection
  corpus/        Corpus storage
  scheduler/     Seed/corpus-entry selection strategies
  seed/          Initial seed generation
  artifact/      Finding storage + deduplication
  minimize/      Test-case (delta) minimization
  llm/           Provider interface for LLM-guided mutation (currently a no-op stub)
  fuzzer/        Orchestration: FuzzerEngine, FuzzerRuntime, FuzzerConfig/Context/Stats
  CLI.java       Entry point
```

## Extending Kai

**Add a mutation** (construction pass): implement `IMutationPolicy` (`targetTypes()`, `id()`, `compatibleWith()`, `apply()`) in `mutation/mutators/`, then register the instance in `FuzzerRuntime.initPolicies()`. Optionally add a nastiness weight in `initMutationNastiness()` to tune how often it's selected.

**Add a destabilizer**: implement `IDestabilizer` (`id()`, `canApply()`, `destabilize()`, and `requiredFlags()` if it needs experimental compiler flags) in `destabilizer/destabilizers/`, then register the instance in `FuzzerRuntime.initDestabilizers()`. Destabilizers must be ADD-only — never remove or rearrange existing AST nodes — and `canApply()` must be side-effect free. Use a guard string (an injection marker checked in `canApply()`) to prevent double-injection on the same corpus entry.

**Add a builder**: implement `IBuilder` in `builders/`. If it should participate in the weighted prototype-matching scheduler used to pick mutation targets, register a representative instance and weight in `FuzzerRuntime.initBuilderWeights()`.

Any compiler flag returned by `requiredFlags()` on a policy or destabilizer is automatically added to the global flag set used by every compile — no wiring needed beyond returning it.

## Status

**v0.2.0-mvp2-destab** — construction pass with ~24 mutators; destabilization pass with 9 destabilizers across 6 K2 attack vectors. Already triggering an open compiler warning (KTLC-365) in early runs. LLM-guided mutation (`llm/`) is scaffolded but not yet implemented.
