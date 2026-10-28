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

## Day 8: benchmark infrastructure

The `benchmark` package provides deterministic inputs and a reusable timing
harness; it does not yet run the full Day 9 experiments or create CSV files.
No measured results are claimed.

- `BenchmarkConfig` defines Small (10 vertices, 30 edges), Medium (100, 500),
  and Large (1000, 5000) datasets, two warm-ups, five measured repetitions, and
  the fixed seed `20261029`.
- `GraphGenerator` creates connected simple undirected weighted graphs,
  simple DAGs, directed cyclic graphs, and connected undirected Dijkstra graphs.
  Weights are integers in `[1, 100]`. The Dijkstra policy is undirected so
  source vertex 0 can reach every vertex.
- `BenchmarkRunner` reuses the same graph for warm-ups and timed repetitions.
  `System.nanoTime()` measures only algorithm execution; graph creation and
  correctness checks are outside the timed interval. Warm-ups are not returned
  as measurements. The harness is an educational microbenchmark, not a formal
  JMH benchmark; JVM warm-up, garbage collection, scheduling, and machine load
  can affect observed times.
- `BenchmarkMetrics` is an immutable record for one measured run, including
  actual edge count, repeat index, time, correctness status, and a snapshot of
  operation counts. `OperationCounter` is a per-execution collector, passed
  only to opt-in instrumented overloads; existing algorithm entry points are
  preserved.

Generated undirected graphs have exactly the requested number of logical edges,
no self-loops or repeated edges, and include a spanning tree. A simple graph
requires `V - 1 <= E <= V(V - 1)/2`. DAG edges only point from a lower vertex ID
to a higher ID, so a simple DAG has at most `V(V - 1)/2` edges (4,950 at
V=100). The assignment specifies a separate 495-edge cap for its standard
Medium DAG, so that dataset uses 495 rather than 500. This is a workload policy,
not the mathematical maximum. `BenchmarkConfig.dagEdgeCount(dataset)` applies
that explicit Medium-dataset cap; other impossible counts are rejected.
The cyclic graph generator includes a cycle of length `V` (a two-vertex cycle
when `V=2`) and then fills to the exact requested count. Impossible edge counts
are rejected rather than silently reduced, except for the explicit DAG dataset
edge-count policy.

Optional counters record executed operations, not theoretical estimates:

- Kruskal: examined candidate edges, union attempts, and successful unions;
  edge examinations stop when its spanning tree is complete.
- Prim: adjacency edge examinations and priority-queue insertions when each
  candidate is enqueued, plus every removal including stale candidates.
- Topological sort: edge examinations in both the in-degree and Kahn passes,
  and vertices removed from the ready queue.
- Kosaraju SCC: DFS edge examinations across both passes and vertices marked
  visited across both passes.
- Dijkstra: examined adjacency edges, all queue removals including stale
  entries, queue insertions including the source, and successful distance
  relaxations.

Operation counts are algorithm-specific and should not be compared across
different algorithms as if they were identical work units. The graph families
and counters provide reproducible inputs and observed work; theoretical
complexity describes asymptotic growth, while empirical time depends on the
input, JVM, hardware, and environment. Day 9 will add experiment orchestration
and CSV summaries; plots and final analysis remain for Day 10.

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
  operation counts, per-run metrics, and the timing harness
- `Main`: graph-model, MST, connectivity, and shortest-path demonstration
- `src/test/java/graph`, `src/test/java/dsu`, `src/test/java/mst`,
  `src/test/java/connectivity`, and `src/test/java/shortestpath`: unit tests
- `src/test/java/benchmark`: benchmark infrastructure tests
