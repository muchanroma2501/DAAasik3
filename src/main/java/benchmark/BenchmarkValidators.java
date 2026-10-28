package benchmark;

import connectivity.TopologicalSortResult;
import graph.Edge;
import graph.WeightedGraph;
import mst.MSTResult;
import shortestpath.ShortestPathResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Correctness checks kept outside timed algorithm executions.
 */
public final class BenchmarkValidators {
    private BenchmarkValidators() {
    }

    public static boolean isValidSpanningTree(WeightedGraph graph, MSTResult result) {
        if (graph.isDirected() || graph.getVertexCount() == 0 || !result.isConnected()
                || result.getEdgeCount() != graph.getVertexCount() - 1) {
            return false;
        }

        Set<Edge> availableEdges = new HashSet<>(graph.getEdges());
        Set<Edge> selectedEdges = new HashSet<>();
        List<List<Integer>> treeAdjacency = new ArrayList<>(graph.getVertexCount());
        for (int vertex = 0; vertex < graph.getVertexCount(); vertex++) {
            treeAdjacency.add(new ArrayList<>());
        }

        long totalWeight = 0;
        for (Edge edge : result.getEdges()) {
            if (!availableEdges.contains(edge) || !selectedEdges.add(edge)) {
                return false;
            }
            totalWeight += edge.getWeight();
            treeAdjacency.get(edge.getSource()).add(edge.getDestination());
            treeAdjacency.get(edge.getDestination()).add(edge.getSource());
        }
        if (totalWeight != result.getTotalWeight()) {
            return false;
        }

        boolean[] visited = new boolean[graph.getVertexCount()];
        Queue<Integer> pending = new ArrayDeque<>();
        visited[0] = true;
        pending.add(0);
        int visitedCount = 0;
        while (!pending.isEmpty()) {
            int vertex = pending.remove();
            visitedCount++;
            for (int neighbor : treeAdjacency.get(vertex)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    pending.add(neighbor);
                }
            }
        }
        return visitedCount == graph.getVertexCount();
    }

    public static boolean areEquivalentMinimumSpanningTrees(
            WeightedGraph graph, MSTResult first, MSTResult second) {
        return isValidSpanningTree(graph, first)
                && isValidSpanningTree(graph, second)
                && first.getTotalWeight() == second.getTotalWeight();
    }

    public static boolean isValidTopologicalOrdering(
            WeightedGraph graph, TopologicalSortResult result) {
        if (!graph.isDirected() || result.hasCycle()
                || result.getOrdering().size() != graph.getVertexCount()) {
            return false;
        }

        int[] positions = new int[graph.getVertexCount()];
        Arrays.fill(positions, -1);
        for (int position = 0; position < result.getOrdering().size(); position++) {
            int vertex = result.getOrdering().get(position);
            if (vertex < 0 || vertex >= graph.getVertexCount() || positions[vertex] >= 0) {
                return false;
            }
            positions[vertex] = position;
        }
        for (Edge edge : graph.getEdges()) {
            if (positions[edge.getSource()] >= positions[edge.getDestination()]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidSccPartition(
            WeightedGraph graph, List<List<Integer>> components) {
        if (!graph.isDirected()) {
            return false;
        }
        boolean[] seen = new boolean[graph.getVertexCount()];
        int count = 0;
        for (List<Integer> component : components) {
            if (component.isEmpty()) {
                return false;
            }
            for (int vertex : component) {
                if (vertex < 0 || vertex >= seen.length || seen[vertex]) {
                    return false;
                }
                seen[vertex] = true;
                count++;
            }
        }
        return count == graph.getVertexCount();
    }

    /**
     * Checks SCC membership against mutual reachability; intended for small test graphs.
     */
    public static boolean sccPartitionMatchesReachability(
            WeightedGraph graph, List<List<Integer>> components) {
        if (!isValidSccPartition(graph, components)) {
            return false;
        }
        boolean[][] reachable = new boolean[graph.getVertexCount()][graph.getVertexCount()];
        for (int source = 0; source < graph.getVertexCount(); source++) {
            Queue<Integer> pending = new ArrayDeque<>();
            reachable[source][source] = true;
            pending.add(source);
            while (!pending.isEmpty()) {
                int vertex = pending.remove();
                for (Edge edge : graph.getAdjacencyList(vertex)) {
                    int destination = edge.getDestination();
                    if (!reachable[source][destination]) {
                        reachable[source][destination] = true;
                        pending.add(destination);
                    }
                }
            }
        }

        int[] componentByVertex = new int[graph.getVertexCount()];
        for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
            for (int vertex : components.get(componentIndex)) {
                componentByVertex[vertex] = componentIndex;
            }
        }
        for (int first = 0; first < graph.getVertexCount(); first++) {
            for (int second = 0; second < graph.getVertexCount(); second++) {
                boolean mutuallyReachable = reachable[first][second] && reachable[second][first];
                if (mutuallyReachable != (componentByVertex[first] == componentByVertex[second])) {
                    return false;
                }
            }
        }
        return true;
    }

    public static long[] calculateReferenceDistances(WeightedGraph graph, int source) {
        if (source < 0 || source >= graph.getVertexCount()) {
            throw new IndexOutOfBoundsException("Invalid reference source: " + source);
        }
        for (Edge edge : graph.getEdges()) {
            if (edge.getWeight() < 0) {
                throw new IllegalArgumentException(
                        "Reference shortest paths require nonnegative weights");
            }
        }
        long[] distances = new long[graph.getVertexCount()];
        Arrays.fill(distances, Long.MAX_VALUE);
        distances[source] = 0;

        for (int pass = 1; pass < graph.getVertexCount(); pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < graph.getVertexCount(); vertex++) {
                if (distances[vertex] == Long.MAX_VALUE) {
                    continue;
                }
                for (Edge edge : graph.getAdjacencyList(vertex)) {
                    int destination = graph.isDirected()
                            ? edge.getDestination() : oppositeEndpoint(edge, vertex);
                    if (distances[vertex] > Long.MAX_VALUE - edge.getWeight()) {
                        continue;
                    }
                    long candidate = distances[vertex] + edge.getWeight();
                    if (candidate < distances[destination]) {
                        distances[destination] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return distances;
    }

    public static boolean isValidShortestPaths(
            WeightedGraph graph, ShortestPathResult result, int source, long[] expectedDistances) {
        if (source < 0 || source >= graph.getVertexCount()
                || result.getSource() != source
                || expectedDistances.length != graph.getVertexCount()
                || result.getDistanceTo(source) != 0
                || expectedDistances[source] != 0) {
            return false;
        }

        for (Edge edge : graph.getEdges()) {
            if (edge.getWeight() < 0) {
                return false;
            }
        }
        for (int target = 0; target < graph.getVertexCount(); target++) {
            long distance = result.getDistanceTo(target);
            if (distance != expectedDistances[target]) {
                return false;
            }
            List<Integer> path = result.getPathTo(target);
            if (distance == Long.MAX_VALUE) {
                if (!path.isEmpty()) {
                    return false;
                }
                continue;
            }
            if (path.isEmpty() || path.get(0) != source
                    || path.get(path.size() - 1) != target) {
                return false;
            }

            long pathCost = 0;
            for (int index = 1; index < path.size(); index++) {
                int from = path.get(index - 1);
                int to = path.get(index);
                if (from < 0 || from >= graph.getVertexCount()
                        || to < 0 || to >= graph.getVertexCount()) {
                    return false;
                }
                long edgeWeight = minimumEdgeWeight(graph, from, to);
                if (edgeWeight == Long.MAX_VALUE) {
                    return false;
                }
                pathCost += edgeWeight;
            }
            if (pathCost != distance) {
                return false;
            }
        }
        return true;
    }

    private static long minimumEdgeWeight(WeightedGraph graph, int source, int destination) {
        long minimum = Long.MAX_VALUE;
        for (Edge edge : graph.getAdjacencyList(source)) {
            int neighbor = graph.isDirected()
                    ? edge.getDestination() : oppositeEndpoint(edge, source);
            if (neighbor == destination) {
                minimum = Math.min(minimum, edge.getWeight());
            }
        }
        return minimum;
    }

    private static int oppositeEndpoint(Edge edge, int vertex) {
        return edge.getSource() == vertex ? edge.getDestination() : edge.getSource();
    }
}
