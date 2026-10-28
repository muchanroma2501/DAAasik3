package mst;

import benchmark.OperationCounter;
import dsu.DSU;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Kruskal's minimum spanning tree / forest algorithm.
 *
 * <p>Sorting costs {@code O(E log E)} and the DSU work costs
 * {@code O(E alpha(V))}; overall time is {@code O(E log E)} and extra space
 * is {@code O(V + E)} for the DSU, sorted candidates, and result.</p>
 */
public final class Kruskal {
    private static final Comparator<Edge> EDGE_ORDER = Comparator
            .comparingInt(Edge::getWeight)
            .thenComparingInt(Edge::getSource)
            .thenComparingInt(Edge::getDestination);

    private Kruskal() {
    }

    /**
     * Computes a minimum spanning tree, or a minimum spanning forest if the
     * undirected input graph is disconnected. The input graph is not modified.
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
            throw new IllegalArgumentException("Kruskal requires an undirected graph");
        }

        int vertexCount = graph.getVertexCount();
        List<Edge> candidates = new ArrayList<>(graph.getEdges());
        candidates.sort(EDGE_ORDER);

        DSU components = new DSU(vertexCount);
        List<Edge> selectedEdges = new ArrayList<>(Math.max(0, vertexCount - 1));
        long totalWeight = 0;
        for (Edge edge : candidates) {
            if (counter != null) {
                counter.recordEdgeExamination();
                counter.recordUnionAttempt();
            }
            if (components.union(edge.getSource(), edge.getDestination())) {
                if (counter != null) {
                    counter.recordSuccessfulUnion();
                }
                selectedEdges.add(edge);
                totalWeight += edge.getWeight();
                if (selectedEdges.size() == vertexCount - 1) {
                    break;
                }
            }
        }

        boolean connected = vertexCount > 0 && components.getComponentCount() == 1;
        return new MSTResult(selectedEdges, totalWeight, connected);
    }
}
