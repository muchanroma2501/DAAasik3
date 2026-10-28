package shortestpath;

import benchmark.OperationCounter;
import graph.Edge;
import graph.WeightedGraph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Computes nonnegative weighted shortest paths using a binary-heap priority
 * queue and lazy duplicate entries.
 *
 * <p>For {@code V} vertices and {@code E} logical edges, the lazy heap can
 * contain {@code O(E + 1)} entries, giving
 * {@code O(V + (E + 1) log(E + 1))} time and {@code O(V + E)} auxiliary space.
 * For simple graphs, this is {@code O((V + E) log V)}.</p>
 */
public final class Dijkstra {
    private static final long INFINITY = Long.MAX_VALUE;

    private Dijkstra() {
    }

    /**
     * Computes shortest paths from a source to every vertex.
     *
     * @param graph directed or undirected graph with nonnegative weights
     * @param source source vertex ID
     * @return immutable shortest distances and path reconstruction data
     * @throws IllegalArgumentException if any graph edge has a negative weight
     * @throws IndexOutOfBoundsException if the source is outside the graph
     * @throws NullPointerException if {@code graph} is null
     */
    public static ShortestPathResult findShortestPaths(WeightedGraph graph, int source) {
        return findShortestPaths(graph, source, null);
    }

    /**
     * Computes shortest paths while optionally collecting operation counts
     * for educational benchmarks.
     *
     * @param graph directed or undirected graph with nonnegative weights
     * @param source source vertex ID
     * @param counter optional operation counter
     * @return immutable shortest distances and path reconstruction data
     */
    public static ShortestPathResult findShortestPaths(WeightedGraph graph, int source,
                                                       OperationCounter counter) {
        int vertexCount = graph.getVertexCount();
        if (source < 0 || source >= vertexCount) {
            throw new IndexOutOfBoundsException(
                    "Source vertex " + source + " is outside [0, " + vertexCount + ")");
        }
        for (Edge edge : graph.getEdges()) {
            if (edge.getWeight() < 0) {
                throw new IllegalArgumentException(
                        "Dijkstra requires nonnegative edge weights: " + edge);
            }
        }

        long[] distances = new long[vertexCount];
        Arrays.fill(distances, INFINITY);
        int[] predecessors = new int[vertexCount];
        Arrays.fill(predecessors, -1);
        distances[source] = 0;

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                Comparator.comparingLong(QueueEntry::distance));
        queue.add(new QueueEntry(source, 0));
        if (counter != null) {
            counter.recordPriorityQueueInsertion();
        }
        while (!queue.isEmpty()) {
            QueueEntry entry = queue.remove();
            if (counter != null) {
                counter.recordPriorityQueuePoll();
            }
            if (entry.distance() != distances[entry.vertex()]) {
                continue;
            }

            int vertex = entry.vertex();
            for (Edge edge : graph.getAdjacencyList(vertex)) {
                if (counter != null) {
                    counter.recordEdgeExamination();
                }
                int neighbor = graph.isDirected()
                        ? edge.getDestination()
                        : oppositeEndpoint(edge, vertex);
                long weight = edge.getWeight();
                if (entry.distance() > Long.MAX_VALUE - weight) {
                    continue;
                }
                long candidate = entry.distance() + weight;
                if (candidate < distances[neighbor]) {
                    distances[neighbor] = candidate;
                    predecessors[neighbor] = vertex;
                    queue.add(new QueueEntry(neighbor, candidate));
                    if (counter != null) {
                        counter.recordSuccessfulRelaxation();
                        counter.recordPriorityQueueInsertion();
                    }
                }
            }
        }

        return new ShortestPathResult(source, distances, predecessors);
    }

    private static int oppositeEndpoint(Edge edge, int vertex) {
        return edge.getSource() == vertex ? edge.getDestination() : edge.getSource();
    }

    private record QueueEntry(int vertex, long distance) {
    }
}
