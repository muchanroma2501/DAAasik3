# Day 10 Visualization and Performance Analysis Report

**Project:** Smart City Infrastructure and Route Planning
**Data source:** Days 8вЂ“9 benchmark CSVs, seed `20261029`
**Python:** 3.15.0
**Matplotlib:** 3.11.2
**Execution date:** 2026-10-29

## Files created and modified

| File | Status | Purpose |
|---|---|---|
| `scripts/plot_results.py` | Created | Locates the project from the script path, validates raw and summary CSVs, checks consistency, and creates the four PNG plots. |
| `scripts/test_plot_results.py` | Created | Standard-library `unittest` validation and plotting tests using temporary copies for invalid cases. |
| `README.md` | Modified | Adds Day 10 setup, run command, plot descriptions, units, data-quality rules, and interpretation limitations. |
| `docs/DAY_10_VISUALIZATION_REPORT.md` | Created | This implementation and execution report. |
| `results/plots/runtime_mean_by_dataset.png` | Generated | Mean runtime by algorithm and categorical dataset. |
| `results/plots/runtime_distribution.png` | Generated | Distributions of five raw measured runs by algorithm and dataset. |
| `results/plots/operation_counts.png` | Generated | Algorithm-specific panels for measured operation counters. |
| `results/plots/runtime_scaling.png` | Generated | Mean runtime across tested dataset categories, including actual V/E annotations. |

The original raw and summary CSV files were read only; neither was regenerated or edited.

## Input schemas and validation

The script uses Python's `csv.DictReader` with strict CSV parsing and UTF-8
decoding. Required raw columns (18): `algorithm`, `dataset`, `graph_family`,
`vertices`, `requested_edges`, `actual_edges`, `seed`, `repeat`, `elapsed_ns`,
`correctness_status`, `edge_examinations`, `priority_queue_polls`,
`priority_queue_insertions`, `successful_relaxations`, `union_attempts`,
`successful_unions`, `vertices_processed`, and `vertices_visited`.

Required summary columns (20): `algorithm`, `dataset`, `graph_family`,
`vertices`, `requested_edges`, `actual_edges`, `seed`, `sample_count`,
`mean_ns`, `median_ns`, `min_ns`, `max_ns`, `mean_edge_examinations`,
`mean_priority_queue_polls`, `mean_priority_queue_insertions`,
`mean_successful_relaxations`, `mean_union_attempts`,
`mean_successful_unions`, `mean_vertices_processed`, and
`mean_vertices_visited`.

Validation checks required headers and field counts, all integer/decimal
parsing and nonnegative values, recognized algorithms/datasets and graph
families, `PASS` correctness statuses, expected seed and graph metadata,
unique repeats 1вЂ“5, and exactly five rows for each of the 15 algorithm/dataset
groups. It verifies all expected groups are present. It separately checks raw
versus summary metadata, sample counts, mean/median/minimum/maximum time, and
every mean operation count; means allow at most `0.000001` difference for
decimal serialization.

The dataset mapping is explicit in the script and matches the Java benchmark
configuration: Small `(V=10, requested E=30)`, Medium `(100,500)`, Large
`(1000,5000)`. Medium topological sort must record **495 actual edges**; the
script rejects 500 actual edges for that case. All raw CSV records are
successful `PASS` measurements and the five warm-ups are absent.

## Generated plots

- `runtime_mean_by_dataset.png`: grouped markers of mean runtime in
  milliseconds by categorical dataset and algorithm. The vertical axis uses a
  symmetric logarithmic scale, so zero-valued measurements remain representable
  and the broad range can be viewed together.
- `runtime_distribution.png`: box plots of the five individual elapsed-time
  samples, in milliseconds, split into three dataset panels. It uses a
  symmetric logarithmic scale; boxes show the median and the triangle marks the
  mean. Raw values exclude warm-ups.
- `operation_counts.png`: means of the relevant counters in separate
  algorithm/counter panels. No counters are summed. Kruskal shows candidate
  edge examinations and DSU union counts; Prim shows adjacency examinations
  and priority-queue activity; topological sort shows edge scans and processed
  vertices; Kosaraju shows traversal scans and visits across both passes;
  Dijkstra shows adjacency scans, queue activity, and successful relaxations.
  The plots explicitly warn that metric counts are not cross-comparable.
- `runtime_scaling.png`: mean runtime in milliseconds across categorical
  Small/Medium/Large tests for each algorithm. Tick annotations come from actual
  CSV vertex and edge counts; the Medium DAG annotation is `V=100, E=495`.
  This visualization describes only the tested inputs and is not evidence of
  asymptotic complexity.

All files were generated at 180 DPI, exceed 160 DPI, and were opened and
verified as PNG images. Dimensions: runtime mean `2000Г—1190`, distribution
`2900Г—1135`, operation counters `3080Г—2720`, scaling `3260Г—1010`.

## Observed values from the actual summary CSV

The table gives the CSV's mean elapsed times converted from nanoseconds to
milliseconds. These are measurements from five repetitions in this environment,
not guaranteed runtimes or generalized performance rankings.

| Algorithm | Small mean (ms) | Medium mean (ms) | Large mean (ms) |
|---|---:|---:|---:|
| Kruskal | 0.0253 | 0.26966 | 2.34538 |
| Prim | 0.06444 | 0.44136 | 2.873 |
| Topological sort | 0.0261 | 0.11776 | 0.80826 |
| Kosaraju SCC | 0.0359 | 0.28986 | 2.68426 |
| Dijkstra | 0.03816 | 0.16008 | 1.46168 |

The Medium topological-sort mean uses 495 actual edges. All other Medium means
use 500 actual edges.

## Tests and commands executed

The test command was:

```text
py -3.15 -m unittest discover -s scripts -p "test_*.py" -v
```

**Result:** 11 tests passed. Coverage includes real CSV validation, missing
files/columns, malformed numbers, failed correctness status, unknown labels,
incorrect repetition counts, summary mismatch, Medium DAG edge validation,
output-directory creation, PNG integrity, input-file hash preservation, and
plotting nonnegative zero-time samples on the symmetric-log axes.

The project-root plot command was:

```text
py -3.15 scripts\plot_results.py
```

**Result:** exit code 0; validated 75 measurements and generated all four
plots, representing 15 summary groups with five measured runs each. The
different-working-directory command was:

```powershell
Push-Location $env:TEMP
py -3.15 C:\Users\user\IdeaProjects\DAAasik3\scripts\plot_results.py
Pop-Location
```

**Result:** exit code 0 with the same four outputs, confirming that default
input/output locations are based on the script location rather than the
current working directory.

The source CSV SHA-256 values before and after plot generation were identical:

- `benchmark_runs.csv`: `DAEF30704305EEDB99EDD1AA66F35EF6A3BD4FB1CC4CF9EAFE20D6092EE1FB6F`
- `benchmark_summary.csv`: `57325E3A497A1C0305F8F211D8A6713DFEBE6096F743530A9BFC5849587165F5`

The Java regression command was run through the locally cached Maven 3.9.9
executable:

```text
C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd test
```

**Result:** `BUILD SUCCESS`; 83 tests run, 0 failures, 0 errors, 0 skipped.
`git diff --check` also passed.

## Limitations and remaining work

Matplotlib was not installed in the initial Python 3.15 environment. It was
installed locally as version 3.11.2 for this work; NumPy and other dependencies
were installed transitively. No Python dependency manifest was added, so a
fresh environment must install Matplotlib using the README command.

The runtime measurements are five-run `System.nanoTime()` observations from the
existing educational harness. Instrumentation counters execute inside the
timed algorithm calls. JVM warm-up, garbage collection, scheduling, hardware,
and system load can affect results. Three categorical dataset sizes cannot
establish asymptotic behavior. Plotting code, generated outputs, and tests are
complete; no Day 10 task remains incomplete.
