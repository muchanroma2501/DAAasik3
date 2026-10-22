# Smart City Infrastructure and Route Planning

This AITU assignment models weighted city infrastructure as graphs and will
eventually compare graph algorithms using tests and reproducible experiments.

## Requirements

- Java 17
- Maven 3.8 or newer

## Implemented features: Days 1-4

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

## Not implemented yet

- Topological sorting
- Cycle detection for directed graphs
- Strongly Connected Components
- Dijkstra's shortest-path algorithm
- Graph generation for experiments
- Performance benchmarking and operation counters
- CSV result tables
- Plots and final experimental analysis

No benchmark results are claimed; experimental features have not been built or
run.

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

The demonstration preserves the Day 1 graph-model output and then prints a
small graph's Kruskal and Prim edges, total weights, connectivity, and whether
the algorithm totals agree.

## Package layout

- `graph`: `Edge` and `WeightedGraph` (implemented on Day 1)
- `dsu`: DSU implementation
- `mst`: Kruskal, Prim, and the shared `MSTResult`
- `Main`: Day 1 graph-model and MST demonstration
- `src/test/java/graph`, `src/test/java/dsu`, and `src/test/java/mst`: graph,
  DSU, and minimum-spanning-tree tests
- Connectivity, shortest-path, graph-generation, and benchmark features remain
  future work
