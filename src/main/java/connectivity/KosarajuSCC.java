package connectivity;

import benchmark.OperationCounter;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Finds strongly connected components in a directed graph using Kosaraju's
 * algorithm.
 *
 * <p>Runs in {@code O(V + E)} time and uses {@code O(V + E)} auxiliary space
 * for the adjacency lists of the transpose and traversal state.</p>
 */
public final class KosarajuSCC {
    private KosarajuSCC() {
    }

    /**
     * Finds all strongly connected components.
     *
     * @param graph directed graph to process
     * @return immutable list of immutable vertex collections, one per component
     * @throws IllegalArgumentException if {@code graph} is undirected
     * @throws NullPointerException if {@code graph} is null
     */
    public static List<List<Integer>> findStronglyConnectedComponents(WeightedGraph graph) {
        return findStronglyConnectedComponents(graph, null);
    }

    /**
     * Finds all strongly connected components while optionally collecting
     * operation counts for educational benchmarks.
     *
     * @param graph directed graph to process
     * @param counter optional operation counter
     * @return immutable list of immutable vertex collections
     */
    public static List<List<Integer>> findStronglyConnectedComponents(
            WeightedGraph graph, OperationCounter counter) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("SCC search requires a directed graph");
        }

        int vertexCount = graph.getVertexCount();
        List<List<Integer>> adjacency = new ArrayList<>(vertexCount);
        List<List<Integer>> transpose = new ArrayList<>(vertexCount);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacency.add(new ArrayList<>());
            transpose.add(new ArrayList<>());
        }
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            for (Edge edge : graph.getAdjacencyList(vertex)) {
                int destination = edge.getDestination();
                adjacency.get(vertex).add(destination);
                transpose.get(destination).add(vertex);
            }
        }

        List<Integer> finishingOrder = finishingOrder(adjacency, counter);
        boolean[] visited = new boolean[vertexCount];
        List<List<Integer>> components = new ArrayList<>();
        for (int index = finishingOrder.size() - 1; index >= 0; index--) {
            int vertex = finishingOrder.get(index);
            if (!visited[vertex]) {
                List<Integer> component = new ArrayList<>();
                collectComponent(vertex, transpose, visited, component, counter);
                components.add(List.copyOf(component));
            }
        }
        return List.copyOf(components);
    }

    private static List<Integer> finishingOrder(List<List<Integer>> adjacency,
                                                OperationCounter counter) {
        boolean[] visited = new boolean[adjacency.size()];
        int[] nextNeighbor = new int[adjacency.size()];
        Deque<Integer> stack = new ArrayDeque<>();
        List<Integer> order = new ArrayList<>(adjacency.size());

        for (int start = 0; start < adjacency.size(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            if (counter != null) {
                counter.recordVertexVisit();
            }
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Integer> neighbors = adjacency.get(vertex);
                if (nextNeighbor[vertex] < neighbors.size()) {
                    int neighbor = neighbors.get(nextNeighbor[vertex]++);
                    if (counter != null) {
                        counter.recordEdgeExamination();
                    }
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        if (counter != null) {
                            counter.recordVertexVisit();
                        }
                        stack.push(neighbor);
                    }
                } else {
                    order.add(stack.pop());
                }
            }
        }
        return order;
    }

    private static void collectComponent(int start, List<List<Integer>> transpose,
                                         boolean[] visited, List<Integer> component,
                                         OperationCounter counter) {
        Deque<Integer> stack = new ArrayDeque<>();
        visited[start] = true;
        if (counter != null) {
            counter.recordVertexVisit();
        }
        stack.push(start);
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            component.add(vertex);
            for (int neighbor : transpose.get(vertex)) {
                if (counter != null) {
                    counter.recordEdgeExamination();
                }
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    if (counter != null) {
                        counter.recordVertexVisit();
                    }
                    stack.push(neighbor);
                }
            }
        }
    }
}
