package benchmark;

import graph.WeightedGraph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic generators for simple weighted graph families. A generated
 * graph contains no self-loops or repeated directed/logical edges.
 */
public final class GraphGenerator {
    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 100;

    private GraphGenerator() {
    }

    public static WeightedGraph generateUndirectedConnectedWeighted(
            int vertices, int edges, long seed) {
        validateUndirectedConnectedSize(vertices, edges);
        WeightedGraph graph = new WeightedGraph(vertices, false);
        Random random = new Random(seed);
        Set<Long> selected = new HashSet<>();

        for (int vertex = 1; vertex < vertices; vertex++) {
            int parent = random.nextInt(vertex);
            addUndirectedEdge(graph, selected, parent, vertex, random);
        }
        addRemaining(graph, selected, undirectedCandidates(vertices), edges, random);
        return graph;
    }

    public static WeightedGraph generateDag(int vertices, int edges, long seed) {
        validateVertexAndEdgeCount(vertices, edges);
        long maximum = (long) vertices * (vertices - 1) / 2;
        requireFeasible(edges, maximum, "simple DAG");
        WeightedGraph graph = new WeightedGraph(vertices, true);
        addRemaining(graph, new HashSet<>(), dagCandidates(vertices), edges, new Random(seed));
        return graph;
    }

    /**
     * Generates a directed graph containing a directed cycle, then fills it to
     * the requested exact logical edge count. At least two vertices and edges
     * are required to form a cycle without self-loops.
     */
    public static WeightedGraph generateDirectedCyclicGraph(
            int vertices, int edges, long seed) {
        validateVertexAndEdgeCount(vertices, edges);
        if (vertices < 2) {
            throw new IllegalArgumentException("A simple directed cycle requires at least 2 vertices");
        }
        long maximum = (long) vertices * (vertices - 1);
        requireFeasible(edges, maximum, "simple directed graph");
        if (edges < 2) {
            throw new IllegalArgumentException("At least 2 edges are required to form a directed cycle");
        }

        WeightedGraph graph = new WeightedGraph(vertices, true);
        Set<Long> selected = new HashSet<>();
        Random random = new Random(seed);
        for (int vertex = 0; vertex < vertices; vertex++) {
            addDirectedEdge(graph, selected, vertex, (vertex + 1) % vertices, random);
        }
        addRemaining(graph, selected, directedCandidates(vertices), edges, random);
        return graph;
    }

    /**
     * Generates a connected undirected weighted graph suitable for Dijkstra.
     * This policy gives every vertex a finite path from source vertex 0.
     */
    public static WeightedGraph generateDijkstraGraph(int vertices, int edges, long seed) {
        return generateUndirectedConnectedWeighted(vertices, edges, seed);
    }

    private static void validateUndirectedConnectedSize(int vertices, int edges) {
        validateVertexAndEdgeCount(vertices, edges);
        if (vertices == 0) {
            throw new IllegalArgumentException("A connected undirected graph needs at least 1 vertex");
        }
        long maximum = (long) vertices * (vertices - 1) / 2;
        requireFeasible(edges, maximum, "simple undirected graph");
        if (edges < vertices - 1) {
            throw new IllegalArgumentException(
                    "A connected graph with " + vertices + " vertices needs at least "
                            + (vertices - 1) + " edges");
        }
    }

    private static void validateVertexAndEdgeCount(int vertices, int edges) {
        if (vertices < 0) {
            throw new IllegalArgumentException("Vertex count cannot be negative: " + vertices);
        }
        if (edges < 0) {
            throw new IllegalArgumentException("Edge count cannot be negative: " + edges);
        }
    }

    private static void requireFeasible(int edges, long maximum, String family) {
        if (edges > maximum) {
            throw new IllegalArgumentException(
                    "Requested " + edges + " edges, but a " + family + " can contain at most "
                            + maximum);
        }
    }

    private static long[] undirectedCandidates(int vertices) {
        long count = (long) vertices * (vertices - 1) / 2;
        long[] candidates = allocateCandidates(count);
        int index = 0;
        for (int source = 0; source < vertices; source++) {
            for (int destination = source + 1; destination < vertices; destination++) {
                candidates[index++] = encode(source, destination, vertices);
            }
        }
        return candidates;
    }

    private static long[] dagCandidates(int vertices) {
        return undirectedCandidates(vertices);
    }

    private static long[] directedCandidates(int vertices) {
        long count = (long) vertices * (vertices - 1);
        long[] candidates = allocateCandidates(count);
        int index = 0;
        for (int source = 0; source < vertices; source++) {
            for (int destination = 0; destination < vertices; destination++) {
                if (source != destination) {
                    candidates[index++] = encode(source, destination, vertices);
                }
            }
        }
        return candidates;
    }

    private static long[] allocateCandidates(long count) {
        if (count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Graph has too many possible edges to generate");
        }
        return new long[(int) count];
    }

    private static void addRemaining(WeightedGraph graph, Set<Long> selected,
                                     long[] candidates, int targetEdges, Random random) {
        for (int index = 0; graph.getEdges().size() < targetEdges; index++) {
            int swapIndex = index + random.nextInt(candidates.length - index);
            long candidate = candidates[swapIndex];
            candidates[swapIndex] = candidates[index];
            candidates[index] = candidate;
            if (!selected.add(candidate)) {
                continue;
            }
            int source = (int) (candidate / graph.getVertexCount());
            int destination = (int) (candidate % graph.getVertexCount());
            graph.addEdge(source, destination, randomWeight(random));
        }
    }

    private static void addUndirectedEdge(WeightedGraph graph, Set<Long> selected,
                                          int source, int destination, Random random) {
        int first = Math.min(source, destination);
        int second = Math.max(source, destination);
        selected.add(encode(first, second, graph.getVertexCount()));
        graph.addEdge(first, second, randomWeight(random));
    }

    private static void addDirectedEdge(WeightedGraph graph, Set<Long> selected,
                                        int source, int destination, Random random) {
        selected.add(encode(source, destination, graph.getVertexCount()));
        graph.addEdge(source, destination, randomWeight(random));
    }

    private static int randomWeight(Random random) {
        return random.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1) + MIN_WEIGHT;
    }

    private static long encode(int source, int destination, int vertices) {
        return (long) source * vertices + destination;
    }
}
