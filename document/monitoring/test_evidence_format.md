# Test Evidence Format — Coverage Improvement Changes

> Applies to: all pull requests that add, update, or move tests for the purpose
> of improving JaCoCo coverage.  Required by spec `test-coverage-improvement`.

---

## 1. Mandatory evidence block

Every coverage-improvement change **MUST** include a test-evidence block in its
PR description or change summary.  The block must contain three sections in
this exact order:

```
### Test evidence

**Tasks run**
```
./gradlew :<module>:testDebugUnitTest            # run JVM unit tests
./gradlew jacocoTestReport                       # rebuild aggregate XML report
./gradlew verifyCoverageBaseline                 # optional: check gates
```

**Coverage delta**
| Scope | Before | After |
|-------|--------|-------|
| LINE  | x.xx % | x.xx % |
| BRANCH | x.xx % | x.xx % |

**Build outcome**
`BUILD SUCCESSFUL` ✓   — or —   `BUILD FAILED` ✗ (reason …)
```

All three sections are required even if the delta is 0.  "I forgot to run it"
is not acceptable; add `jacocoTestReport` to local Gradle task chain before
sending.

---

## 2. Obtaining coverage numbers

Run the parse helper from the repo root:

```bash
# Windows
python parse_coverage.py

# Linux / macOS
python3 parse_coverage.py
```

The script reads `build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
and prints:

1. **Overall summary** — LINE / BRANCH / CLASS / METHOD ratios
2. **Module breakdown** — per-module LINE coverage sorted ascending
3. **Hot-spot table** — lowest-covered packages with ≥ 50 executable lines
4. **High-coverage packages** — for context

Record the *"Overall"* LINE and BRANCH figures from section 1 for the
*Before* and *After* rows in the evidence table.

---

## 3. Scoped runs (single-module changes)

When a change only touches one or two modules, prefer scoped commands to
reduce CI time:

| Module touched | Recommended command |
|---------------|------------------|
| `local_component`, `anime_component` | `./gradlew :local_component:testDebugUnitTest :anime_component:testDebugUnitTest jacocoTestReport` |
| `storage_component` | `./gradlew :storage_component:testDebugUnitTest jacocoTestReport` |
| `player_component` | `./gradlew :player_component:testDebugUnitTest jacocoTestReport` |
| `core_*` modules | `./gradlew :<module>:testDebugUnitTest jacocoTestReport` |
| Instrumentation-only | `./gradlew connectedDebugAndroidTest` (requires a device/emulator in `device` state) |

---

## 4. Baseline gate

The `verifyCoverageBaseline` Gradle task enforces that priority-module
coverage does not regress below the recorded baseline.  Run it after adding
new tests to confirm that you have not accidentally removed coverage:

```bash
./gradlew verifyCoverageBaseline
```

A `BUILD SUCCESSFUL` result here means no baseline regression.  If the task
fails, the violation output names the exact package and the gap (e.g.
*"Rule violated… covered ratio is 0.12 but expected minimum is 0.15"*).

Raise the gate threshold once you have confirmed the new floor is stable — do
**not** lower it to make a failing build pass.

---

## 5. Skipping the gate (last resort)

If you must merge without satisfying a gate (e.g. a test environment issue,
not a real regression), add a `// COVERAGE_GATE_SKIP: <reason>` comment
next to the temporarily lowered limit in `build.gradle.kts` and create a
follow-up tracking issue.  Do **not** silently disable a rule.

---

## 6. TV / instrumentation paths

For changes that affect TV-reachable flows, add a DPAD-path evidence line:

```
**TV regression**
Path: PlayHistoryActivity launch → BACK → finish
Driver: adb + DPAD (KEYCODE_BACK)
Result: activity reached DESTROYED state — PASS
```

See [`document/Contributing.md`](../Contributing.md) and the AGENTS.md TV/Remote
UX section for the full DPAD self-check checklist.
