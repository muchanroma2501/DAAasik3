package connectivity;

import benchmark.OperationCounter;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Computes a topological ordering of a directed graph using Kahn's algorithm.
 *
 * <p>Runs in {@code O(V + E)} time and uses {@code O(V)} auxiliary space.</p>
 */
public final class TopologicalSort {
    private TopologicalSort() {
    }

    /**
     * Computes a topological ordering, or reports a cycle if no ordering exists.
     *
     * @param graph directed graph to process
     * @return complete topological ordering, or an empty ordering if cyclic
     * @throws IllegalArgumentException if {@code graph} is undirected
     * @throws NullPointerException if {@code graph} is null
     */
    public static TopologicalSortResult sort(WeightedGraph graph) {
        return sort(graph, null);
    }

    /**
     * Computes a topological ordering while optionally collecting operation
     * counts for educational benchmarks.
     *
     * @param graph directed graph to process
     * @param counter optional operation counter
     * @return complete ordering, or an empty ordering if cyclic
     */
    public static TopologicalSortResult sort(WeightedGraph graph, OperationCounter counter) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological sort requires a directed graph");
        }

        int vertexCount = graph.getVertexCount();
        int[] inDegree = new int[vertexCount];
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            for (Edge edge : graph.getAdjacencyList(vertex)) {
                if (counter != null) {
                    counter.recordEdgeExamination();
                }
                inDegree[edge.getDestination()]++;
            }
        }

        Queue<Integer> ready = new ArrayDeque<>();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            if (inDegree[vertex] == 0) {
                ready.add(vertex);
            }
        }

        List<Integer> ordering = new ArrayList<>(vertexCount);
        while (!ready.isEmpty()) {
            int vertex = ready.remove();
            if (counter != null) {
                counter.recordVertexProcessed();
            }
            ordering.add(vertex);
            for (Edge edge : graph.getAdjacencyList(vertex)) {
                if (counter != null) {
                    counter.recordEdgeExamination();
                }
                int destination = edge.getDestination();
                inDegree[destination]--;
                if (inDegree[destination] == 0) {
                    ready.add(destination);
                }
            }
        }

        boolean hasCycle = ordering.size() != vertexCount;
        return new TopologicalSortResult(hasCycle ? List.of() : ordering, hasCycle);
    }
}
