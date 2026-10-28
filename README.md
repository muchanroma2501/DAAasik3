# Smart City Infrastructure and Route Planning

This AITU assignment models weighted city infrastructure as graphs and
implements graph algorithms with unit tests and a lightweight educational
benchmark infrastructure.

## Requirements

- Java 17
- Maven 3.8 or newer

## Implemented features: Days 1-7

Day 1 provides an immutable integer-weighted `Edge`, a directed or undirected
`WeightedGraph` with an edge list and adjacency lists, a small runnable
demonstration, and graph-model unit tests. Zero-vertex and single-vertex graphs
are supported; duplicate edges and self-loops are allowed.

Day 2 adds `dsu.DSU`, a reusable integer-ID disjoint-set data structure. It uses
path compression in `find` and union by size in `union`, giving amortized
`O(alpha(V))` time per operation and `O(V)` storage.

Day 3 adds Kruskal's algorithm. It sorts the logical edge list by increasing
weight and uses DSU to accept only edges that join distinct components. Its
time complexity is `O(E log E)` and its extra space is `O(V + E)`.

Day 4 adds Prim's algorithm, using adjacency lists and a Java `PriorityQueue`
to repeatedly consider the lightest crossing edge. It restarts for each
unvisited vertex, so disconnected graphs produce a minimum spanning forest.
With duplicate heap candidates, its safe general time bound is
`O(E log E + V)` and extra space is `O(E + V)`.

Both algorithms return the shared immutable `MSTResult`: a read-only list of
selected edges, a `long` total weight, and a connected status. For a
disconnected graph, the result is a minimum spanning forest and `connected` is
false. A one-vertex graph is connected with a zero-edge tree; a zero-vertex
graph is considered disconnected because no spanning tree exists. The
algorithms support negative weights and do not modify their input graph.

## Day 5: Topological sorting and directed-cycle detection

`connectivity.TopologicalSort.sort(graph)` applies Kahn's algorithm to a
directed graph. A topological ordering places the source of each edge before
its destination; such an ordering exists only for a directed acyclic graph.
The algorithm counts every edge occurrence in each vertex's in-degree, so
duplicate edges are handled correctly. If a cycle remains, the result reports
`hasCycle()` and returns an empty ordering rather than a partial one.
Undirected inputs are rejected with `IllegalArgumentException`.

```java
WeightedGraph dag = new WeightedGraph(3, true);
dag.addEdge(0, 1, 1);
dag.addEdge(1, 2, 1);
TopologicalSortResult result = TopologicalSort.sort(dag);
System.out.println(result.getOrdering()); // [0, 1, 2]
System.out.println(result.hasCycle());    // false
```

The runtime is `O(V + E)` and auxiliary space is `O(V)`.

## Day 6: Strongly connected components

`connectivity.KosarajuSCC.findStronglyConnectedComponents(graph)` returns each
strongly connected component as an immutable list of vertex IDs. A component is
a maximal set where every vertex can reach every other vertex. Kosaraju's
algorithm first records DFS finishing order in the original directed graph,
then runs DFS in reverse finishing order on the transposed graph (all edges
reversed). The transpose makes each second-pass traversal collect exactly one
component. Undirected inputs are rejected.

```java
List<List<Integer>> components =
        KosarajuSCC.findStronglyConnectedComponents(directedGraph);
System.out.println(components);
```

The runtime is `O(V + E)` and auxiliary space is `O(V + E)` to store the
transpose and traversal state.

## Day 7: Dijkstra shortest paths

`shortestpath.Dijkstra.findShortestPaths(graph, source)` computes shortest
distances from a source in either a directed or undirected graph. It uses a
priority queue and skips stale entries; a result provides the source, all
distances, reachability checks, and immutable reconstructed paths. Distances
use `long`, with `Long.MAX_VALUE` denoting unreachable vertices. Any negative
edge weight is rejected, even if the edge is disconnected from the source.
Undirected traversal determines the opposite endpoint from each stored edge's
original source/destination.

```java
WeightedGraph routes = new WeightedGraph(3, false);
routes.addEdge(0, 1, 4);
routes.addEdge(1, 2, 3);
ShortestPathResult paths = Dijkstra.findShortestPaths(routes, 0);
System.out.println(paths.getDistanceTo(2)); // 7
System.out.println(paths.getPathTo(2));     // [0, 1, 2]
```

With lazy duplicate priority-queue entries, runtime is
`O(V + (E + 1) log(E + 1))` and auxiliary space is `O(V + E)`. For simple
graphs, the time bound is `O((V + E) log V)`.

## Days 8–9: benchmark infrastructure and experiments

The `benchmark` package contains deterministic generators, the reusable
`BenchmarkRunner`, independent result validators, an experiment coordinator,
and raw/summary CSV output. `BenchmarkExperimentMain` runs Kruskal, Prim,
topological sort, Kosaraju SCC, and Dijkstra on Small (10 vertices), Medium
(100), and Large (1000) datasets. Undirected workloads request 30, 500, and
5000 logical edges, respectively. All graph weights are integers in `[1,100]`.

The seed is fixed at `20261029`; each generator uses a deterministic
`java.util.Random` seeded with that value. The experiment reuses each generated
graph for warm-ups and measured repetitions. It performs two warm-up executions
and five measured executions for each algorithm/dataset pair. Warm-ups are not
stored in the returned measurements or CSV files.

Graph policies:

- Kruskal and Prim use the same connected, simple, undirected weighted graph
  for each dataset. The generator inserts a spanning tree and then unique
  logical edges, with no loops or duplicate edges.
- Topological sort uses a simple DAG whose edges point from lower to higher
  vertex IDs. The Medium target of 500 is capped to **495 actual edges** by
  `BenchmarkConfig.dagEdgeCount`; this is the documented workload cap. The
  mathematical maximum for a simple 100-vertex DAG is 4,950 edges.
- Kosaraju SCC uses a simple directed graph with a two-edge directed cycle and
  fills to the exact dataset edge count without loops or duplicate edges. SCCs
  need not contain all vertices in a single component.
- Dijkstra uses a connected, simple, undirected weighted graph, so source vertex
  0 reaches every vertex and all weights are nonnegative.

`BenchmarkExperiment` verifies the actual vertex and logical-edge counts before
running an algorithm. Before timing, it also verifies MST agreement, rejects a
cyclic topological-sort input, and computes independent reference shortest
distances. For every warm-up and measured result, validation checks MST tree
structure and baseline weight, every topological-order constraint, complete
non-overlapping SCC partitioning, or Dijkstra distances and reconstructed path
edges/costs. Any validation failure throws and prevents result-file writing.
Small-graph tests additionally compare SCC groups to mutual reachability.

`BenchmarkRunner` measures only the algorithm function with
`System.nanoTime()`. Graph generation, precomputed references, and result
validation occur outside each timed interval. Per-run counter snapshots are
created after timing. The algorithm execution itself includes the opt-in
counter null checks and increments; reported times therefore include
instrumentation overhead and should not be interpreted as uninstrumented
production timings.

Operation counters record performed work, with algorithm-specific definitions:

- Kruskal: candidate-edge examinations, DSU union attempts, successful unions.
  The edge loop stops after the tree is complete; sorting and internal DSU
  `find` calls are not counted.
- Prim: adjacency entries examined/enqueued and every priority-queue removal,
  including stale candidates.
- Topological sort: edge scans in both the initial indegree pass and Kahn pass;
  vertices removed from the ready queue.
- Kosaraju SCC: DFS edge examinations and first-time vertex marks in both DFS
  passes. A vertex may count once in each pass; transpose construction is not
  counted.
- Dijkstra: examined adjacency edges, queue insertions (including the source),
  queue removals (including stale entries), and successful distance
  relaxations. The pre-run negative-weight scan is not counted.

Counters describe algorithm-specific operations and are not directly
comparable between different algorithms. Timing summaries are empirical
observations, not guaranteed runtime. This is a lightweight educational
`System.nanoTime()` benchmark, not JMH; JVM compilation, garbage collection,
scheduling, and machine load can affect results.

Run the complete experiment from the project root after building:

```text
mvn package
java -cp target/classes benchmark.BenchmarkExperimentMain
```

The main class creates `results/tables` as needed and overwrites both files on
each complete run:

- `results/tables/benchmark_runs.csv`: one row for each of the 75 measured
  executions; columns are `algorithm`, `dataset`, `graph_family`, `vertices`,
  `requested_edges`, `actual_edges`, `seed`, `repeat`, `elapsed_ns`,
  `correctness_status`, `edge_examinations`, `priority_queue_polls`,
  `priority_queue_insertions`, `successful_relaxations`, `union_attempts`,
  `successful_unions`, `vertices_processed`, and `vertices_visited`.
- `results/tables/benchmark_summary.csv`: one row per algorithm/dataset group
  (15 rows), with sample count, mean/median/minimum/maximum nanoseconds, and
  mean values for all operation-counter columns. The even-sample median is the
  arithmetic mean of the two middle sorted values; means are rounded to six
  decimal places and trailing zeros are omitted.

The output directory can be overridden with an optional command-line argument,
for example `java -cp target/classes benchmark.BenchmarkExperimentMain custom/results`.
The fixed seed and dataset definitions reproduce graph inputs;
elapsed times naturally vary by runtime environment.

## Day 10: visualization and performance analysis

The Python visualization script validates the Days 8–9 raw and summary CSVs
before plotting. It rejects missing columns/files, malformed numeric data,
unexpected algorithm or dataset labels, incorrect repetition counts, failed
correctness statuses, graph metadata errors, and mismatches between raw runs
and summary statistics. It uses Python's CSV parser, so CSV quoting is handled
without altering the source data.

Install Matplotlib if it is not already available:

```text
python -m pip install matplotlib
```

Generate plots from the project root:

```text
python scripts/plot_results.py
```

The script locates input CSVs relative to its own location, so it can also be
called by absolute script path from another working directory. It creates
`results/plots/` automatically. The script reads but never rewrites:
`results/tables/benchmark_runs.csv` (75 individual measured rows, five per
algorithm/dataset) and `results/tables/benchmark_summary.csv` (15 grouped
summaries).

Generated files:

- `runtime_mean_by_dataset.png`: grouped mean-runtime markers for the five
  algorithms across the categorical Small, Medium, and Large datasets.
- `runtime_distribution.png`: box plots of the five raw measured times for
  every algorithm, split into one panel per dataset; warm-ups are excluded.
- `operation_counts.png`: per-algorithm small multiples of relevant operation
  counter means. Panels state the specific counter; separate metrics are not
  summed or represented as a common “total operations” value.
- `runtime_scaling.png`: each algorithm's runtime across the three tested
  dataset categories, annotated with vertex and **actual** edge counts. The
  Medium DAG is displayed as `E=495` (requested 500), not 500 actual edges.

Runtime plots use milliseconds and a symmetric logarithmic vertical scale to
show the range while retaining valid handling of zero-valued measurements.
The distribution plot uses the individual nanosecond measurements converted
to milliseconds for display; the CSV values remain unchanged. The summary
statistics are derived from raw runs: mean, median, minimum, and maximum are
shown/available in the summary CSV, while distribution charts preserve
run-to-run variability. The operation plots use the documented definitions:
Kruskal edge examinations, DSU union attempts, and successful unions; Prim
adjacency examinations and queue polls/insertions; topological-sort edge
examinations and processed vertices; SCC traversal edge examinations and
visits across both passes; Dijkstra adjacency examinations, queue polls and
insertions, and successful relaxations. These operation categories are
algorithm-specific and are not directly comparable or interchangeable.

The plotted values are five-run `System.nanoTime()` measurements from the
lightweight educational harness, not a JMH-grade benchmark. Instrumentation
counter checks occur inside the timed algorithm call, and hardware, JVM state,
garbage collection, scheduling, and environment load can affect results. Three
dataset categories show observed behavior only; they do not prove asymptotic
complexity. Re-running the script against the same CSV inputs produces the
same plotted values and filenames; rendering metadata may vary with the local
Matplotlib/font environment.

## Build, test, and run

```text
mvn test
mvn package
```

To run the demonstration from IntelliJ, run `Main.main()`. Alternatively, after
packaging, run it with the compiled classes on the classpath:

```text
java -cp target/classes Main
```

The demonstration preserves the Day 1 graph-model output and prints the MST,
topological-sort, cycle-detection, SCC, and shortest-path examples.

## Package layout

- `graph`: `Edge` and `WeightedGraph` (implemented on Day 1)
- `dsu`: DSU implementation
- `mst`: Kruskal, Prim, and the shared `MSTResult`
- `connectivity`: Kahn's topological sort and Kosaraju's SCC implementation
- `shortestpath`: Dijkstra's algorithm and immutable `ShortestPathResult`
- `benchmark`: benchmark configuration, deterministic graph generation,
  validators, experiment orchestration, operation counts, CSV output, and
  timing/summary metrics
- `scripts/plot_results.py`: input validation and Days 8–9 result visualization
- `Main`: graph-model, MST, connectivity, and shortest-path demonstration
- `src/test/java/graph`, `src/test/java/dsu`, `src/test/java/mst`,
  `src/test/java/connectivity`, and `src/test/java/shortestpath`: unit tests
- `src/test/java/benchmark`: benchmark infrastructure tests
