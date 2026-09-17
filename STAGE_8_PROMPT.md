# AppProbe — Stage 8: Performance Analysis & Leak-Suspicion Detection

You are working on the existing **AppProbe** Android project.

Before making any changes, inspect the existing codebase and `progress.md`. Stages 1–7 are already completed and verified.

## Stage 8 Goal

Build the first **performance-analysis layer** on top of the existing Stage 7 monitoring data.

The goal is to analyze collected `PerformanceSample` data and identify **suspicious memory-growth patterns** that may indicate a possible memory-retention issue.

### Important distinction

Stage 8 must **NOT claim that a memory leak has been definitively detected**.

The system should report evidence such as:

* Memory growth detected
* Memory remains elevated after repeated cycles
* Increasing memory trend
* Possible retention pattern
* Insufficient data for analysis

Use terminology such as:

> "Potential memory retention detected"

or

> "Suspicious memory growth pattern"

Never present the result as proof of a memory leak.

---

# 1. Inspect Existing Architecture First

Before implementation:

* Inspect `PerformanceSample.kt`
* Inspect `PerformanceMonitor.kt`
* Inspect `ExecutionResult.kt`
* Inspect `ExecutionEngine.kt`
* Inspect `ScenarioExecutionViewModel.kt`
* Inspect `ScenarioExecutionScreen.kt`
* Inspect `TestAction.kt`
* Inspect `TestScenario.kt`
* Inspect `progress.md`

Understand the existing data flow.

Do NOT rewrite the Stage 6 execution engine.

Do NOT replace the Stage 7 monitoring architecture.

Reuse the existing `PerformanceSample`, `MonitoringSummary`, and `ScenarioExecutionResult` models wherever possible.

---

# 2. Create Performance Analysis Models

Create an appropriate model under:

`app/src/main/java/com/appprobe/analysis/`

For example:

`PerformanceAnalysisResult.kt`

The analysis result should contain useful evidence such as:

* sample count
* initial Total PSS
* final Total PSS
* minimum Total PSS
* maximum Total PSS
* average Total PSS
* absolute memory growth
* percentage memory growth
* number of execution cycles if derivable
* trend information
* analysis confidence/data sufficiency
* suspicion status

Create a small enum/sealed model for the analysis result, for example:

* `INSUFFICIENT_DATA`
* `STABLE`
* `MEMORY_GROWTH_DETECTED`
* `POSSIBLE_MEMORY_RETENTION`

Choose names that fit the existing architecture.

Keep the models simple and extensible.

---

# 3. Implement PerformanceAnalyzer

Create:

`app/src/main/java/com/appprobe/analysis/PerformanceAnalyzer.kt`

This component must be independent from UI.

Input:

`List<PerformanceSample>`

Output:

`PerformanceAnalysisResult`

The analyzer should:

1. Validate that sufficient samples exist.
2. Ignore invalid/unavailable metric values safely.
3. Calculate:

   * minimum Total PSS
   * maximum Total PSS
   * average Total PSS
   * initial Total PSS
   * final Total PSS
   * absolute growth
   * percentage growth
4. Determine whether memory is generally increasing.
5. Detect whether memory remains elevated toward the end of execution.
6. Produce a conservative analysis classification.

Do NOT use an arbitrary single sample to declare a leak.

Do NOT make conclusions from CPU or thread count alone.

Do NOT introduce machine learning.

Do NOT introduce external dependencies.

---

# 4. Use a Conservative Analysis Strategy

The analysis should account for normal memory fluctuation.

Do NOT simply implement:

`finalMemory > initialMemory => memory leak`

Instead, consider:

* minimum observed memory
* maximum observed memory
* average memory
* final memory
* overall trend
* number of valid samples

A short execution with only a few samples should normally produce:

`INSUFFICIENT_DATA`

rather than a strong conclusion.

A temporary memory spike followed by recovery should not automatically be classified as retention.

A sustained upward trend should produce a result such as:

`MEMORY_GROWTH_DETECTED`

A repeated/sustained elevated pattern may produce:

`POSSIBLE_MEMORY_RETENTION`

The thresholds should be centralized and clearly documented so they can be refined later.

Do not over-engineer the statistical model in Stage 8.

---

# 5. Add Analysis to Execution Results

Extend:

`ScenarioExecutionResult`

so the completed execution can contain the Stage 8 analysis result.

For example:

`performanceAnalysis: PerformanceAnalysisResult?`

Do not remove or replace:

* `performanceSamples`
* `monitoringSummary`

Stage 7 evidence must remain available.

The intended pipeline is:

Scenario
→ ExecutionEngine
→ PerformanceMonitor
→ PerformanceSamples
→ PerformanceAnalyzer
→ PerformanceAnalysisResult

---

# 6. Integrate Analyzer With ExecutionEngine

After execution and monitoring have completed:

1. Stop the performance monitor safely.
2. Obtain the collected samples.
3. Generate the existing monitoring summary.
4. Pass the samples to `PerformanceAnalyzer`.
5. Attach the analysis result to `ScenarioExecutionResult`.

Maintain the existing cancellation and `finally` cleanup behavior.

Do not allow analysis failures to crash scenario execution.

If analysis fails, return a safe `INSUFFICIENT_DATA`/unavailable result where appropriate.

---

# 7. Update ScenarioExecutionScreen

Add a new UI section after the existing:

**MONITORING SUMMARY**

Create:

**PERFORMANCE ANALYSIS**

Display:

* Analysis status
* Initial Total PSS
* Final Total PSS
* Memory growth
* Growth percentage
* Trend
* Number of valid samples

Example conceptual output:

```text
PERFORMANCE ANALYSIS

Status
Possible Memory Retention

Initial PSS
142 MB

Final PSS
181 MB

Memory Growth
+39 MB

Growth
+27.4%

Trend
Increasing

Samples Analyzed
12
```

For insufficient data:

```text
PERFORMANCE ANALYSIS

Status
Insufficient Data

Reason
Not enough valid memory samples were collected.
```

Use clear, neutral wording.

Do not display:

"Memory Leak Detected"

unless a future stage introduces a much stronger detection methodology.

---

# 8. Keep Stage 7 Live Monitoring Unchanged

While execution is running, continue showing the existing:

**RESOURCE MONITOR**

Do not replace the live monitoring UI.

Stage 8 analysis should primarily operate on completed execution data.

Avoid adding expensive continuous analysis during execution.

---

# 9. Re-run Behavior

Verify that:

* Every new scenario execution gets fresh performance samples.
* Previous analysis results are cleared before a new run.
* Re-run generates a new analysis result.
* Cancelled executions do not incorrectly display a successful analysis.
* Insufficient samples are handled safely.

---

# 10. Error Handling

The application must remain stable if:

* target process disappears
* PID becomes invalid
* memory information becomes unavailable
* samples contain unavailable values
* scenario is cancelled
* monitoring returns zero samples
* analyzer receives invalid/insufficient data

No crash should occur because of analysis failure.

---

# 11. Dependencies

Do NOT add new dependencies unless absolutely necessary.

Prefer:

* Kotlin standard library
* existing coroutines
* existing Compose/Material 3 dependencies

No ML framework is required for Stage 8.

No backend is required.

No database is required.

---

# 12. Testing Requirements

After implementation:

### Build

Run:

```bash
./gradlew assembleDebug
```

The build must succeed with:

* 0 errors
* no new unnecessary warnings

### Install

Use the existing emulator:

`Pixel_6`

API 35.

Install the generated APK.

### Runtime Test

Run a scenario such as:

```text
Launch App
Wait 2000 ms
Press Back
Launch App
```

Verify:

* execution works
* monitoring collects samples
* analysis is generated
* monitoring summary remains visible
* performance analysis appears
* no crash occurs

### Re-run Test

Run the scenario again.

Verify that:

* previous samples do not leak into the new run
* analysis is regenerated
* sample count is fresh

### Cancellation Test

Start a longer scenario.

Cancel execution.

Verify:

* execution stops
* monitoring stops
* no orphan coroutine remains
* no crash occurs
* cancelled execution is not falsely reported as a successful leak analysis

### Logcat

Run:

```bash
adb logcat -d '*:E' | grep -i com.appprobe
```

Expected:

```text
Clean!
```

or no AppProbe-related errors.

---

# 13. Code Quality Requirements

Follow the existing architecture.

Use clear separation:

```text
testing/
    TestAction
    TestScenario
    ExecutionEngine
    ExecutionResult

monitoring/
    PerformanceMonitor
    PerformanceSample

analysis/
    PerformanceAnalyzer
    PerformanceAnalysisResult
```

Do not duplicate existing models.

Do not move files unnecessarily.

Do not introduce a repository/database layer for Stage 8.

Keep the analyzer deterministic and unit-testable.

---

# 14. Important Points to Remember

* Implement ONLY Stage 8.
* Do not implement future stages.
* Do not redesign the existing application.
* Do not rewrite Stage 6 execution logic.
* Do not replace Stage 7 monitoring.
* Reuse existing models and dependencies.
* Inspect files before creating new ones.
* Do not create duplicate classes.
* Keep changes small and focused.
* Preserve existing navigation.
* Preserve existing Stage 7 UI.
* Preserve cancellation behavior.
* Do not claim definitive memory-leak detection.
* Treat this stage as **evidence analysis**, not proof of a leak.
* Avoid arbitrary thresholds scattered throughout the code; centralize them.
* Do not add ML or backend infrastructure.
* Do not add speculative features.
* Verify the actual application on the emulator after implementation.
* Update `progress.md` only after the implementation and verification are actually complete.

---

# 15. Token Efficiency

Use tokens efficiently.

* First inspect only the files relevant to Stage 8.
* Do not dump entire files unless necessary.
* Prefer targeted searches and relevant code sections.
* Reuse existing implementations instead of recreating them.
* Before creating a class/function, search the project to ensure it does not already exist.
* Make small focused edits.
* Do not repeatedly print unchanged files.
* After changes, report only:

  * files created/modified
  * important implementation decisions
  * build result
  * runtime verification result
  * remaining issues
* Avoid huge logcat dumps.
* Avoid repeating the entire project structure unless it changed.
* Do not spend tokens explaining obvious Kotlin/Compose syntax.
* Keep the final implementation summary concise but technically precise.

---

# Final Goal

AppProbe should progressively become a local Android application testing and performance-analysis tool:

```text
Discover Apps
      ↓
Inspect App
      ↓
Select Target
      ↓
Build Test Scenario
      ↓
Execute Scenario
      ↓
Collect Performance Evidence
      ↓
Analyze Performance Evidence
      ↓
Identify Suspicious Memory Patterns
      ↓
Future: Deeper Memory-Leak Investigation
```

Stage 8 is ONLY the:

**Collect Performance Evidence → Analyze Performance Evidence**

part of this pipeline.

Do not implement the future deeper memory-leak investigation stage yet.
