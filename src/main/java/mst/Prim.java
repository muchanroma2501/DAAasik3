package mst;

import benchmark.OperationCounter;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Prim's minimum spanning tree / forest algorithm using a binary heap.
 *
 * <p>This implementation supports disconnected graphs by starting Prim again
 * at each unvisited vertex. With adjacency lists and duplicate heap entries,
 * the safe general bound is {@code O(E log E + V)} time and {@code O(E + V)}
 * extra space; this assumes edge weights and endpoint IDs use constant-time
 * comparisons.</p>
 */
public final class Prim {
    private static final Comparator<Edge> EDGE_ORDER = Comparator
            .comparingInt(Edge::getWeight)
            .thenComparingInt(Edge::getSource)
            .thenComparingInt(Edge::getDestination);

    private Prim() {
    }

    /**
     * Computes a minimum spanning tree or forest, starting at vertex 0 when
     * present and then covering any other components. The input graph is not
     * modified.
     *
     * @param graph undirected weighted graph
     * @return selected edges, their total weight, and spanning-tree status
     * @throws IllegalArgumentException if {@code graph} is directed
     * @throws NullPointerException if {@code graph} is null
     */
    public static MSTResult findMst(WeightedGraph graph) {
        return findMst(graph, null);
    }

    /**
     * Computes a minimum spanning tree or forest while optionally collecting
     * operation counts for educational benchmarks.
     *
     * @param graph undirected weighted graph
     * @param counter optional operation counter
     * @return selected edges, their total weight, and spanning-tree status
     */
    public static MSTResult findMst(WeightedGraph graph, OperationCounter counter) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }
        return findMst(graph, 0, false, counter);
    }

    /**
     * Computes a minimum spanning tree or forest, beginning at the requested
     * source and then covering any remaining components.
     *
     * @param graph undirected weighted graph
     * @param source starting vertex for the first component
     * @return selected edges, their total weight, and spanning-tree status
     * @throws IllegalArgumentException if {@code graph} is directed
     * @throws IndexOutOfBoundsException if the source is outside the graph
     * @throws NullPointerException if {@code graph} is null
     */
    public static MSTResult findMst(WeightedGraph graph, int source) {
        return findMst(graph, source, null);
    }

    /**
     * Computes a minimum spanning tree or forest while optionally collecting
     * operation counts for educational benchmarks.
     *
     * @param graph undirected weighted graph
     * @param source starting vertex for the first component
     * @param counter optional operation counter
     * @return selected edges, their total weight, and spanning-tree status
     */
    public static MSTResult findMst(WeightedGraph graph, int source, OperationCounter counter) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }
        return findMst(graph, source, true, counter);
    }

    private static MSTResult findMst(WeightedGraph graph, int source, boolean validateSource,
                                     OperationCounter counter) {
        int vertexCount = graph.getVertexCount();
        if (validateSource && (source < 0 || source >= vertexCount)) {
            throw new IndexOutOfBoundsException(
                    "Source vertex " + source + " is outside [0, " + vertexCount + ")");
        }

        boolean[] visited = new boolean[vertexCount];
        PriorityQueue<Edge> candidates = new PriorityQueue<>(EDGE_ORDER);
        List<Edge> selectedEdges = new ArrayList<>(Math.max(0, vertexCount - 1));
        long totalWeight = 0;

        if (vertexCount > 0) {
            totalWeight = growComponent(graph, source, visited, candidates, selectedEdges,
                    totalWeight, counter);
        }
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            if (!visited[vertex]) {
                totalWeight = growComponent(graph, vertex, visited, candidates, selectedEdges,
                        totalWeight, counter);
            }
        }

        boolean connected = vertexCount > 0 && selectedEdges.size() == vertexCount - 1;
        return new MSTResult(selectedEdges, totalWeight, connected);
    }

    private static long growComponent(WeightedGraph graph, int start, boolean[] visited,
                                      PriorityQueue<Edge> candidates, List<Edge> selectedEdges,
                                      long totalWeight, OperationCounter counter) {
        visited[start] = true;
        addAdjacentCandidates(graph, start, candidates, counter);

        while (!candidates.isEmpty()) {
            Edge edge = candidates.remove();
            if (counter != null) {
                counter.recordPriorityQueuePoll();
            }
            int nextVertex = visited[edge.getSource()]
                    ? edge.getDestination()
                    : edge.getSource();
            if (visited[nextVertex]) {
                continue;
            }

            visited[nextVertex] = true;
            selectedEdges.add(edge);
            totalWeight += edge.getWeight();
            addAdjacentCandidates(graph, nextVertex, candidates, counter);
        }

        return totalWeight;
    }

    private static void addAdjacentCandidates(WeightedGraph graph, int vertex,
                                              PriorityQueue<Edge> candidates,
                                              OperationCounter counter) {
        for (Edge edge : graph.getAdjacencyList(vertex)) {
            if (counter != null) {
                counter.recordEdgeExamination();
                counter.recordPriorityQueueInsertion();
            }
            candidates.add(edge);
        }
    }
}
