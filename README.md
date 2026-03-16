# Kai — Kotlin Compiler Fuzzer

Kai is a structure-aware, grey-box fuzzer for the Kotlin compiler. It operates at the AST level — generating and mutating Kotlin programs via a builder tree, and submitting them to `kotlinc` in search of Internal Compiler Errors (ICEs), crashes, and other unexpected behaviors.

---

## How It Works

Instead of generating random text, Kai builds a mutable in-memory program tree and applies structured mutations to it. Each mutation is a small, incremental perturbation inspired by **Monte Carlo Markov Chain** exploration — the fuzzer stays in the neighborhood of structurally interesting programs rather than jumping to random ones.

```
SyntheticSeedProvider
        │
        ▼
  ProgramBuilder tree  ──►  MutationChain  ──►  kotlinc subprocess
        │                        │                      │
        │                   chain.json             CompilerResult
        │                  (replay log)                  │
        ▼                                           IceOracle
   ArtifactStore  ◄──── OracleVerdict.Finding ─────────┘
  crashes/crash_0001/
    program.kt
    stderr.log
    chain.json
    verdict.json
```

Every crash is **fully reproducible** — the seed ID, RNG seed, NameRegistry snapshot, and full mutation chain are recorded in `chain.json`. Replaying a crash is deterministic.

---

## Architecture

Kai is fully pluggable. Every strategy is an interface — swapping an implementation means changing one field in `FuzzerContext`. The core loop never changes.

| Interface | Responsibility | MVP Implementation |
|---|---|---|
| `IBuilder` | Node in the program tree | 6 builder types |
| `IMutationPolicy` | Single structural mutation | 6 policies |
| `ISeedProvider` | Supplies base programs | `SyntheticSeedProvider` |
| `IOracle` | Evaluates compiler output | `IceOracle` + `CrashOracle` |
| `ICoverageCollector` | Tracks compiler code paths | `NoOpCoverageCollector` |
| `IScheduler` | Selects seeds and policies | `RandomScheduler` |
| `ICorpusManager` | Manages seed pool | `SimpleCorpusManager` |
| `IMinimizer` | Reduces crash reproducers | `NoOpMinimizer` |

---

## Builder Tree

The program tree represents a Kotlin program in memory. Mutations operate exclusively on this tree — never on emitted source.

```
ProgramBuilder
├── ClassBuilder
│   ├── FunctionBuilder
│   │   ├── VariableBuilder
│   │   ├── BranchBuilder
│   │   └── LoopBuilder
│   └── VariableBuilder
└── FunctionBuilder
    └── ExpressionBuilder
```

---

## MVP Mutation Policies

| Policy | Target | Description |
|---|---|---|
| `AddVariableMutation` | `FunctionBuilder`, `BranchBuilder` | Inserts a new `val`/`var` |
| `AddLoopMutation` | `FunctionBuilder`, `BranchBuilder` | Wraps body in `for`/`while` |
| `ExpandExpressionMutation` | `ExpressionBuilder` | Replaces literal with binary op |
| `AddFunctionMutation` | `ClassBuilder`, `ProgramBuilder` | Adds a new member function |
| `GenericMutation` | `ClassBuilder`, `FunctionBuilder` | Injects a type parameter |
| `InjectNullCheckMutation` | `FunctionBuilder`, `BranchBuilder` | Adds nullable variable |

---

## Target Failure Classes

| Failure | Detection | Priority |
|---|---|---|
| Internal Compiler Error (ICE) | stderr pattern + stack trace origin | P0 |
| Compiler crash / segfault | Non-zero exit + no ICE marker | P0 |
| Compilation hang | Process timeout | P1 |
| K1 vs K2 divergence | Differential oracle (planned) | P1 |

---

## Requirements

- JDK 24+
- Kotlin compiler (`kotlinc`) 2.3.10+
- Gradle 9+

---

## Build

```bash
./gradlew build
```

---

## Run

```bash
java -jar build/libs/kai-mvp-1.0-SNAPSHOT.jar \
  -kotlinc /path/to/kotlinc \
  -log ./crashes
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

---

## Crash Artifacts

Every finding is stored under `-log`:

```
crashes/
└── crash_0001/
    ├── program.kt          ← triggering Kotlin source
    ├── stderr.log          ← full compiler stderr + stack trace
    ├── chain.json          ← seed ID, RNG seed, mutation chain (for replay)
    └── verdict.json        ← FindingType, description, timestamp
```

---

## Project Structure

```
src/main/java/io/kai/
├── artifact/       ArtifactStore
├── builders/       ProgramBuilder, ClassBuilder, FunctionBuilder, ...
├── compiler/       CompilerRunner, IOracle, OracleVerdict, oracles/
├── contracts/      IBuilder, BuildContext, NameRegistry, capability/
├── corpus/         ICorpusManager, SimpleCorpusManager, CorpusMeta
├── fuzzer/         FuzzerEngine, FuzzerContext, FuzzerConfig
├── llm/            ILLMProvider, NoOpLLMProvider
├── minimize/       IMinimizer, NoOpMinimizer
├── mutation/       IMutationPolicy, MutationRegistry, chain/, mutators/
├── scheduler/      IScheduler, RandomScheduler
└── seed/           ISeedProvider, SyntheticSeedProvider
```

---

## Status

**v0.1.0-mvp** — Core loop functional. Generates valid Kotlin, invokes `kotlinc` as a subprocess, detects ICEs and crashes, stores artifacts with full replay information.

**Planned for v0.2.0:**
- 45 additional mutation policies targeting Kotlin-specific ICE patterns (reified/inline, recursive generics, coroutines, sealed classes)
- `DeltaMinimizer` for crash reduction
- `BanditScheduler` for coverage-guided mutation selection
- `JacocoCoverageCollector` for grey-box mode
- `DifferentialOracle` for K1 vs K2 divergence detection
- PSI ingestion for external seed programs

---

## Design References

- Mutation chain design inspired by MCMC perturbation — specifically the state-perturbation model from Metropolis Light Transport and Athena (Le et al., 2015)
- Oracle design based on kotlinc ICE detection patterns from the Kotlin issue tracker