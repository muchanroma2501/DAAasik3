# Smart City Infrastructure and Route Planning

This AITU assignment models weighted city infrastructure as graphs and will
eventually compare graph algorithms using tests and reproducible experiments.

## Requirements

- Java 17
- Maven 3.8 or newer

## Current implementation: Day 1

Day 1 provides an immutable integer-weighted `Edge`, a directed or undirected
`WeightedGraph` with an edge list and adjacency lists, a small runnable
demonstration, and graph-model unit tests. Zero-vertex and single-vertex graphs
are supported; duplicate edges and self-loops are allowed.

## Planned work (not implemented yet)

- Disjoint Set Union, Kruskal, and Prim for minimum spanning trees
- Topological sorting, cycle detection, and strongly connected components
- Dijkstra's shortest-path algorithm
- Graph generation, metrics, benchmarks, CSV output, and plots

Experimental results will be added only after the experiments are implemented
and actually executed.

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

## Package layout

- `graph`: `Edge` and `WeightedGraph` (implemented on Day 1)
- `Main`: the Day 1 demonstration
- `src/test/java/graph`: graph model tests
- `dsu`, `mst`, `connectivity`, `shortestpath`, and `benchmark`: planned
  locations for later implementation; these packages are not created yet
