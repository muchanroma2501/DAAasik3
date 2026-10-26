# Smart City Infrastructure and Route Planning

This AITU assignment models weighted city infrastructure as graphs and
implements graph algorithms with unit tests. Benchmarking and experimental
analysis are reserved for a later stage.

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

## Not implemented yet

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

The demonstration preserves the Day 1 graph-model output and prints the MST,
topological-sort, cycle-detection, SCC, and shortest-path examples.

## Package layout

- `graph`: `Edge` and `WeightedGraph` (implemented on Day 1)
- `dsu`: DSU implementation
- `mst`: Kruskal, Prim, and the shared `MSTResult`
- `connectivity`: Kahn's topological sort and Kosaraju's SCC implementation
- `shortestpath`: Dijkstra's algorithm and immutable `ShortestPathResult`
- `Main`: graph-model, MST, connectivity, and shortest-path demonstration
- `src/test/java/graph`, `src/test/java/dsu`, `src/test/java/mst`,
  `src/test/java/connectivity`, and `src/test/java/shortestpath`: unit tests
- Graph generation and benchmark features remain future work
