# AppProbe — Stage 7: Performance & Resource Monitoring

## Context

AppProbe is an Android application that discovers installed applications, allows the user to inspect an application, select it as a testing target, build a test scenario, and execute that scenario against the target application.

The following stages are already completed and verified:

* Stage 1 — Project Foundation
* Stage 2 — Application Discovery
* Stage 3 — Application Inspection
* Stage 4 — Target Application Selection
* Stage 5 — Test Scenario Configuration
* Stage 6 — Test Scenario Execution

The current execution flow is:

Discovery → Inspection → Target Selection → Scenario Builder → Scenario Execution

Stage 6 already executes the configured actions sequentially and reports action-level execution status.

The purpose of Stage 7 is to add a **resource/performance monitoring layer around scenario execution**.

Do NOT implement memory-leak detection in this stage.

Memory growth is evidence that will later be analyzed by a dedicated analysis stage. Increasing memory usage alone must NOT be reported as a memory leak.

---

# Primary Goal

Build a reusable monitoring system that collects timestamped performance/resource measurements from the target application's process while a test scenario is executing.

The monitoring system must integrate with the existing Stage 6 execution architecture without unnecessarily rewriting the existing execution engine.

The architecture should conceptually become:

Scenario
↓
Execution Engine
↓
Action execution
↓
Resource Monitor
↓
Timestamped metrics
↓
Stored scenario monitoring data

---

# 1. Inspect Existing Architecture First

Before changing anything:

1. Inspect the existing Stage 1–6 implementation.
2. Read `progress.md`.
3. Inspect the existing:

   * `TestAction`
   * `TestScenario`
   * `ExecutionResult`
   * `ExecutionEngine`
   * `ScenarioExecutionViewModel`
   * `ScenarioExecutionScreen`
   * target application models/state
4. Determine the cleanest integration point for monitoring.
5. Do not duplicate existing models or create parallel execution architecture.
6. Preserve existing Stage 6 behavior.

Do not rewrite working functionality simply to introduce the monitoring layer.

---

# 2. Create Performance Monitoring Models

Create appropriate models under a suitable package such as:

`com.appprobe.monitoring`

The exact package structure may be adjusted if the existing architecture suggests a better location.

Create a model representing one resource measurement.

It should contain, at minimum:

* timestamp
* target package name
* process ID
* memory information
* CPU information where reliably obtainable
* thread/process information where obtainable
* associated scenario action/index when applicable

Use strongly typed Kotlin models rather than generic maps.

For example, conceptually:

```text
PerformanceSample
 ├── timestamp
 ├── packageName
 ├── pid
 ├── actionIndex
 ├── actionType
 ├── javaHeapUsed
 ├── javaHeapAllocated
 ├── nativeHeapUsed
 ├── totalMemory
 ├── cpuUsage
 └── threadCount
```

Do not blindly copy these fields if Android's available APIs do not provide them reliably. Use the most reliable Android APIs available to the application.

---

# 3. Memory Monitoring

Implement a memory monitoring component for the target process.

Prefer Android-supported APIs such as:

* `ActivityManager`
* `Debug`
* `Debug.MemoryInfo`
* `/proc/<pid>/...` only where appropriate and reliably accessible

The monitor should attempt to collect useful memory measurements such as:

* Java/Dalvik heap
* native heap
* total PSS / proportional memory where available
* other useful process memory values available through Android APIs

Clearly distinguish different memory measurements.

Do NOT present incompatible memory units as if they were equivalent.

Use `Long` values internally where appropriate and keep the original unit clear.

---

# 4. CPU Monitoring

Add CPU measurement where it can be obtained reliably for the target process.

Possible sources include:

* Android process APIs
* `/proc/<pid>/stat`
* `/proc/<pid>/status`
* other Android-compatible mechanisms

If CPU percentage cannot be calculated reliably at a particular point, represent the measurement as unavailable rather than inventing a value.

The implementation should not crash because a metric cannot be read.

---

# 5. Thread Monitoring

Collect the target process thread count when available.

This can be obtained through appropriate Android process information or `/proc` information where permitted.

Again, failure to collect the metric must not crash the test.

---

# 6. Monitoring Service / Collector

Create a dedicated monitoring abstraction.

For example:

```text
PerformanceMonitor
```

or an equivalent architecture that fits the existing project.

It should provide functionality conceptually similar to:

```text
startMonitoring(targetPackage)
captureSample(...)
stopMonitoring()
```

The exact API is up to the implementation.

Important:

* Monitoring must run off the UI thread.
* Do not block Compose rendering.
* Use coroutines appropriately.
* Handle cancellation safely.
* Do not leak monitoring coroutines.
* Stop monitoring when scenario execution is cancelled or completed.

---

# 7. Sampling Strategy

Implement a reasonable sampling strategy.

The monitor should periodically collect measurements during scenario execution.

Use a configurable sampling interval rather than hardcoding the value throughout the application.

A reasonable initial interval is approximately:

```text
500 ms – 1000 ms
```

Choose a sensible default.

The sampling mechanism must:

* start when execution begins
* collect samples while execution is active
* stop when execution finishes
* stop when execution is cancelled
* avoid excessive CPU usage itself

Do not collect data at extremely high frequency.

---

# 8. Associate Metrics With Scenario Actions

This is an important requirement.

Performance samples should be associated with the currently executing scenario action when possible.

For example:

```text
Scenario
    Step 1: Launch App
        samples...
    Step 2: Wait
        samples...
    Step 3: Swipe
        samples...
    Step 4: Press Back
        samples...
```

Extend the existing execution progress architecture only where necessary.

Do not break the existing Stage 6 action statuses.

---

# 9. Scenario-Level Monitoring Result

Extend the execution result architecture so that a completed scenario can contain monitoring information.

Conceptually:

```text
ScenarioExecutionResult
 ├── targetPackage
 ├── scenarioName
 ├── startTimestamp
 ├── endTimestamp
 ├── stepResults
 ├── performanceSamples
 └── overallStatus
```

Keep this model extensible because later stages will analyze these samples.

Do not implement leak detection or advanced analysis here.

---

# 10. UI — Basic Monitoring Visibility

Update the existing Scenario Execution screen to display basic monitoring information.

Do NOT create a complicated analytics dashboard yet.

While execution is running, show a small monitoring section containing useful live values such as:

* Memory
* CPU
* Threads
* Sample count

Example:

```text
RESOURCE MONITOR

Memory       142 MB
CPU           18%
Threads       27
Samples       14
```

The UI must clearly indicate that these are **current measurements**, not leak conclusions.

After execution completes, show a compact summary such as:

```text
Monitoring Summary

Samples collected: 126
Peak memory: 184 MB
Minimum memory: 96 MB
Average memory: 137 MB
Peak CPU: 42%
Peak threads: 31
```

Only display values that are actually available.

Do not invent unavailable metrics.

---

# 11. Keep Stage 6 Functionality Intact

The following Stage 6 functionality must continue working:

* Launch App
* Wait
* Tap
* Swipe
* Press Back
* Rotate
* Background App
* Resume App
* sequential execution
* cancellation
* re-run
* action status updates
* execution completion
* failure handling

Do not regress existing functionality.

---

# 12. Error Handling

Monitoring must be non-fatal.

If a resource measurement fails:

* record the metric as unavailable where appropriate
* continue monitoring other metrics
* continue scenario execution
* do not crash the application

The execution engine must remain functional even if monitoring partially fails.

---

# 13. Performance Considerations

AppProbe is itself a monitoring application, so avoid making the monitor excessively expensive.

Important:

* Do not sample every few milliseconds.
* Do not perform expensive PackageManager operations on every sample.
* Do not repeatedly resolve the target application unnecessarily.
* Do not perform blocking operations on the main thread.
* Do not retain unnecessary historical objects indefinitely.
* Avoid excessive Compose recomposition from every raw sample if unnecessary.

Use an efficient representation for monitoring data.

---

# 14. Testing Requirements

After implementation:

### Build

Run:

```bash
./gradlew assembleDebug
```

The build must succeed with zero errors.

### Emulator

Use:

```text
Pixel_6
API 35
```

Install the generated APK and launch AppProbe.

### Functional test

Create a simple scenario such as:

```text
Launch App
Wait 2000 ms
Press Back
Launch App
```

Verify:

* scenario starts
* monitoring starts
* samples are collected
* resource values appear
* action statuses continue updating
* monitoring stops after completion
* scenario completes successfully

### Cancellation test

Start a longer scenario and cancel it.

Verify:

* execution stops
* monitoring stops
* no monitoring coroutine remains active
* no crash occurs

### Re-run test

Run the same scenario again.

Verify:

* a fresh monitoring session starts
* old samples do not incorrectly appear as part of the new run
* execution and monitoring both complete normally

### Logcat

Run:

```bash
adb logcat -d '*:E' | grep -i com.appprobe || echo "Clean! No AppProbe crash or errors found."
```

There should be no AppProbe crashes or uncaught exceptions.

---

# 15. Important Architectural Constraint

Do NOT implement:

* memory leak detection
* leak probability
* leak scoring
* automatic leak diagnosis
* ML-based leak detection
* historical comparison
* charts
* report generation

Those belong to later stages.

Stage 7 is strictly:

**Collect reliable evidence.**

Stage 8 can analyze that evidence.

---

# 16. Update progress.md

After successful implementation and verification, update `progress.md` with:

```text
Stage 7: Performance & Resource Monitoring — COMPLETED
```

Include:

* monitoring architecture
* metrics collected
* sampling strategy
* execution integration
* UI changes
* build result
* emulator verification
* cancellation verification
* re-run verification
* logcat result
* any known limitations

Do not mark Stage 7 complete until the implementation has actually been built and tested.

---

# 17. Final Response Requirements

After implementation, report:

1. Files created
2. Files modified
3. Monitoring architecture
4. Metrics actually collected
5. Sampling interval
6. How monitoring integrates with Stage 6
7. UI changes
8. Build result
9. Emulator test result
10. Cancellation test result
11. Re-run test result
12. Logcat result
13. Known limitations
14. Whether Stage 7 is fully complete

Do not claim tests were performed if they were not actually performed.

---

# Important Points to Remember

* AppProbe is an Android application, not a web application.
* Stage 6 is already the scenario execution foundation.
* Stage 7 adds monitoring around that execution.
* Do not rewrite the Stage 6 execution engine unnecessarily.
* Do not equate memory growth with a memory leak.
* Monitoring failures must never crash the target test.
* Keep monitoring lightweight.
* Keep UI responsive.
* Keep the architecture extensible for Stage 8 analysis.
* Inspect existing code before creating new classes.
* Reuse existing models and state where appropriate.
* Avoid duplicate implementations.
* Do not modify unrelated parts of the project.
* Do not add unnecessary dependencies.

---

# Token Efficiency

Because AI coding-agent usage is limited:

1. **Inspect before implementing.**
   Read only the relevant existing Stage 6 files first.

2. **Do not dump entire project files.**
   Inspect targeted files/classes instead.

3. **Do not repeatedly run huge logcat commands.**
   Prefer:

```bash
adb logcat -d '*:E' | grep -i com.appprobe
```

4. **Build after logical milestones**, not after every tiny edit.

5. **Do not repeatedly rediscover the project architecture.**
   `progress.md` is the project history.

6. **Do not rewrite working Stage 1–6 code.**

7. **Keep implementation incremental:**

   * models
   * monitor
   * execution integration
   * UI
   * testing

8. If a build error occurs, inspect the specific compiler error and relevant file rather than dumping the entire project.

9. Keep emulator verification focused on the current stage.

10. At the end, provide a concise implementation summary instead of dumping large source files or logs.

---

# Definition of Done

Stage 7 is complete only when:

* [ ] Performance monitoring models exist
* [ ] Target-process memory monitoring works
* [ ] CPU monitoring works where available
* [ ] Thread monitoring works where available
* [ ] Sampling runs asynchronously
* [ ] Samples are associated with scenario execution
* [ ] Monitoring starts/stops with execution
* [ ] Cancellation stops monitoring correctly
* [ ] Re-run creates a fresh monitoring session
* [ ] Basic live metrics are visible in the execution UI
* [ ] Execution results contain monitoring data
* [ ] Stage 6 functionality still works
* [ ] APK builds successfully
* [ ] Pixel_6/API 35 verification succeeds
* [ ] Logcat is clean
* [ ] `progress.md` is updated
* [ ] No leak detection is incorrectly claimed
