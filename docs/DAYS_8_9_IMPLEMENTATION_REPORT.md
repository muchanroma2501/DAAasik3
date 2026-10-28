# Days 8вЂ“9 Implementation and Experiment Report

**Project:** Smart City Infrastructure and Route Planning
**Runtime:** OpenJDK 17.0.20.1 (Microsoft build)
**Seed:** `20261029`
**Execution date:** 2026-10-29

## Implementation

Day 8's directed cyclic graph generator was corrected. It now seeds a directed two-cycle (`0 -> 1 -> 0`) and adds distinct directed edges until the requested edge count is reached. It accepts exactly `2 <= E <= V(V-1)` for `V >= 2`, creates no self-loops or duplicate directed edges, and remains deterministic for an identical seed. Boundary tests cover `E=2`, `E=V-1`, `E=V`, and a larger request.

The Day 9 layer is implemented in the existing `benchmark` package:

| Class/file | Responsibility |
|---|---|
| `BenchmarkExperimentMain` | Runnable entry point; accepts an optional result directory and defaults to `results/tables`. |
| `BenchmarkExperiment` | Generates standard workload graphs, checks their actual metadata, runs all five algorithms, and returns all validated measured rows. |
| `BenchmarkValidators` | Validates spanning trees and MST agreement, topological orderings, SCC partitions, and Dijkstra results; includes a BellmanвЂ“Ford reference-distance calculation and a small-graph SCC reachability comparison. |
| `BenchmarkCsvWriter` | Escapes CSV text, creates output directories, overwrites the two output files, and writes raw runs and grouped summaries in UTF-8. |
| `BenchmarkSummary` | Calculates sample count, mean, median, minimum, maximum, and means for every operation-counter field. |
| `BenchmarkConfig`, `BenchmarkMetrics`, `BenchmarkRunner`, `GraphGenerator`, `OperationCounter` | Existing Day 8 configuration, record, timing, generation, and instrumentation facilities reused by the experiment. |
| `BenchmarkInfrastructureTest` | Adds cyclic-generator boundary coverage and retains existing infrastructure assertions. |
| `BenchmarkExperimentTest` | Tests correctness validators, all 15 algorithm/dataset groups, CSV escaping/schema, row counts, summary calculations, and failure handling. |
| `README.md` | Documents experiment command, dataset policies, validation, CSV schema, counters, and timing limitations. |

No existing algorithm implementation was replaced. The experiment calls the existing Kruskal, Prim, topological-sort, Kosaraju SCC, and Dijkstra methods through their optional instrumentation overloads. The existing public non-instrumented entry points remain available.

## Workloads and validation

The standard configuration has two warm-up executions and five measured executions for each algorithm/dataset pair. A measured run is timed with `System.nanoTime()` around the algorithm call only. Graph generation, precomputed reference values, correctness validation, and CSV writing are outside that measured interval. The algorithm call does include the optional counter checks and increments.

All graphs use seed `20261029`. Actual logical edge counts observed in each run are:

| Algorithm | Graph family | Small (V=10) | Medium (V=100) | Large (V=1000) |
|---|---|---:|---:|---:|
| Kruskal | Connected undirected weighted | 30 | 500 | 5000 |
| Prim | Connected undirected weighted | 30 | 500 | 5000 |
| Topological sort | Directed acyclic | 30 | **495** | 5000 |
| Kosaraju SCC | Directed cyclic | 30 | 500 | 5000 |
| Dijkstra | Connected undirected weighted | 30 | 500 | 5000 |

For the Medium DAG, `requested_edges` remains the generic target 500, while the actual workload is capped to 495 by `BenchmarkConfig.dagEdgeCount`. This is a workload choice; a simple 100-vertex DAG has a mathematical maximum of 4,950 edges. All other listed workload counts are exact.

Before the measurements are accepted:

- Kruskal and Prim must each return a connected tree with `V-1` valid input edges and a matching selected-edge weight sum. Their total MST weights must agree. Each timed result is validated against that baseline weight.
- The DAG is checked to be acyclic before the run. Every topological result is checked for one occurrence of every vertex and correct order for every edge.
- SCC output is checked as a complete, non-overlapping partition of all vertices. A unit test independently compares small-graph component membership with mutual reachability.
- Dijkstra input weights are nonnegative. Independent BellmanвЂ“Ford distances are computed before timing; each result must match those distances, have source distance zero, and provide paths with valid graph edges and matching costs.

The benchmark runner also validates warm-up results. Any failed validation throws; the experiment does not write a successful row for it. The SCC experiment verifies partition structure at runtime rather than computing all-pairs reachability for the large datasets.

Operation fields are actual instrumented events: edge examinations, priority-queue polls and insertions, successful relaxations, union attempts and successful unions, processed vertices, and visited vertices. Kruskal sorting and DSU internal `find` calls are not counted; SCC transpose construction and Dijkstra's initial negative-weight scan are also excluded. Queue-poll counters include stale removals. SCC visits count first-time visits in each pass, so a vertex can contribute in both passes. Counters are algorithm-specific, not common comparable units.

## Test and build execution

The complete Maven suite was run through the Maven 3.9.9 executable found in the local Maven wrapper distribution cache:

```text
C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd test
```

**Result:** `BUILD SUCCESS`; 83 tests run, 0 failures, 0 errors, 0 skipped. The suite includes the existing Day 1вЂ“7 regression tests and the new benchmark tests. A full IntelliJ project rebuild also reported success with no build problems.

The experiment was built and run with:

```text
C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd package
java -cp target\classes benchmark.BenchmarkExperimentMain
```

**Result:** Maven packaging succeeded, including all 83 tests; the experiment process exited with code 0 and reported 75 completed measured runs. The measured experiment process took 269 ms in this environment. This elapsed figure is only the coordinator's wall-clock execution duration, not a per-algorithm benchmark result.

## Generated CSV files and independent checks

The experiment created/overwrote:

- `results/tables/benchmark_runs.csv` вЂ” 75 data rows plus a header; 7,635 bytes.
- `results/tables/benchmark_summary.csv` вЂ” 15 data rows plus a header; 2,027 bytes.

The raw file contains 18 columns: algorithm, dataset, graph family, vertices, requested/actual edges, seed, measured repeat, elapsed nanoseconds, correctness status, and all eight operation-counter fields. Each algorithm/dataset pair has repeat indices 1вЂ“5, each status is `PASS`, and no warm-up rows are present. The summary contains one row per algorithm/dataset pair and 20 columns, including sample count, mean/median/minimum/maximum time, and all eight mean counter values. Medium topological-sort rows record requested edges 500 and actual edges 495.

Both files were re-imported with PowerShell's CSV parser. A separate verification recomputed, for each of the 15 groups, the sample count, mean, median, minimum, maximum, and every mean counter from raw rows and compared them to the summary. It also checked the headers, total row counts, 5-run group counts, all PASS statuses, repeat indices, seeds, graph families, and edge metadata. **Every check passed.** The odd five-sample medians were also verified in the raw/summary comparison; unit tests cover both odd and even median inputs.

Observed time summaries from this run (nanoseconds; environment-specific):

| Algorithm | Dataset | Mean | Median | Minimum | Maximum |
|---|---|---:|---:|---:|---:|
| Kruskal | Small | 25300 | 22000 | 21100 | 35400 |
| Kruskal | Medium | 269660 | 279900 | 249700 | 287300 |
| Kruskal | Large | 2345380 | 2373400 | 2228300 | 2496800 |
| Prim | Small | 64440 | 60100 | 56500 | 84100 |
| Prim | Medium | 441360 | 398100 | 384600 | 534200 |
| Prim | Large | 2873000 | 2852700 | 2408900 | 3282200 |
| Topological sort | Small | 26100 | 20600 | 19600 | 48600 |
| Topological sort | Medium | 117760 | 118000 | 113500 | 123200 |
| Topological sort | Large | 808260 | 690200 | 259000 | 1514200 |
| Kosaraju SCC | Small | 35900 | 35700 | 31500 | 40300 |
| Kosaraju SCC | Medium | 289860 | 286300 | 281100 | 310000 |
| Kosaraju SCC | Large | 2684260 | 2657700 | 2545600 | 2865200 |
| Dijkstra | Small | 38160 | 35000 | 30100 | 53500 |
| Dijkstra | Medium | 160080 | 154300 | 148400 | 186500 |
| Dijkstra | Large | 1461680 | 1331000 | 579600 | 2721000 |

These are real measurements from the execution above, but they are not guaranteed runtimes or a formal performance ranking. This lightweight harness is not JMH; JVM compilation state, garbage collection, scheduling, environment load, and instrumentation overhead affect the observations.

## Limitations

- The MST correctness check validates tree structure and agreement between two independent algorithm implementations; it is not an exhaustive independent optimality oracle.
- Timing includes instrumentation overhead and should not be read as uninstrumented method cost.
- The five-run sample is small and the run duration is environment-dependent.
- Plotting and graphical analysis remain Day 10 work.
- The earlier `docs/DAYS_8_9_IMPLEMENTATION_AUDIT.md` is a point-in-time audit of the pre-Day-9 state; this report records the subsequent implementation and execution.
