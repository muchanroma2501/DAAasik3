package shortestpath;

import graph.Edge;
import graph.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraTest {
    @Test
    void computesKnownShortestDistances() {
        WeightedGraph graph = graph(5, true, new int[][]{
                {0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}, {3, 4, 3}
        });

        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(List.of(0L, 3L, 1L, 4L, 7L), result.getDistances());
    }

    @Test
    void prefersIndirectRouteOverMoreExpensiveDirectEdge() {
        WeightedGraph graph = graph(3, true, new int[][]{
                {0, 2, 20}, {0, 1, 4}, {1, 2, 3}
        });

        assertEquals(7, Dijkstra.findShortestPaths(graph, 0).getDistanceTo(2));
    }

    @Test
    void selectsBestOfCompetingRoutesAndReconstructsPath() {
        WeightedGraph graph = graph(5, true, new int[][]{
                {0, 1, 3}, {1, 4, 8}, {0, 2, 2}, {2, 3, 1}, {3, 4, 2}, {0, 4, 10}
        });
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(5, result.getDistanceTo(4));
        assertValidPath(graph, result, 4);
    }

    @Test
    void respectsDirectionAndDoesNotFollowReverseOnlyEdge() {
        WeightedGraph graph = graph(3, true, new int[][]{{1, 0, 2}, {1, 2, 5}});
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertFalse(result.isReachable(1));
        assertFalse(result.isReachable(2));
        assertTrue(Dijkstra.findShortestPaths(graph, 1).isReachable(2));
    }

    @Test
    void correctlyTraversesUndirectedEdgeRegardlessOfStoredOrientation() {
        WeightedGraph graph = graph(3, false, new int[][]{{1, 0, 6}, {2, 1, 2}});
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(6, result.getDistanceTo(1));
        assertEquals(8, result.getDistanceTo(2));
        assertValidPath(graph, result, 2);
    }

    @Test
    void representsUnreachableAndSourcePathsCorrectly() {
        WeightedGraph graph = graph(3, true, new int[][]{{0, 1, 1}});
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(0, result.getDistanceTo(0));
        assertEquals(List.of(0), result.getPathTo(0));
        assertFalse(result.isReachable(2));
        assertEquals(Long.MAX_VALUE, result.getDistanceTo(2));
        assertEquals(List.of(), result.getPathTo(2));
    }

    @Test
    void supportsZeroWeightEdges() {
        WeightedGraph graph = graph(3, true, new int[][]{{0, 1, 0}, {1, 0, 0}, {1, 2, 4}});
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(0, result.getDistanceTo(1));
        assertEquals(4, result.getDistanceTo(2));
        assertValidPath(graph, result, 2);
    }

    @Test
    void handlesParallelEdgesAndSelfLoops() {
        WeightedGraph graph = graph(3, false, new int[][]{
                {0, 0, 0}, {0, 1, 9}, {0, 1, 2}, {1, 2, 3}, {2, 2, 1}
        });
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(2, result.getDistanceTo(1));
        assertEquals(5, result.getDistanceTo(2));
        assertValidPath(graph, result, 2);
    }

    @Test
    void rejectsNegativeEdgeWeightsAnywhereInTheGraph() {
        WeightedGraph connectedNegative = graph(2, true, new int[][]{{0, 1, -1}});
        WeightedGraph disconnectedNegative = graph(4, true, new int[][]{{2, 3, -1}});

        assertThrows(IllegalArgumentException.class,
                () -> Dijkstra.findShortestPaths(connectedNegative, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Dijkstra.findShortestPaths(disconnectedNegative, 0));
    }

    @Test
    void rejectsInvalidSourceAndTargetIds() {
        WeightedGraph graph = new WeightedGraph(2, true);
        assertThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.findShortestPaths(graph, -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.findShortestPaths(graph, 2));

        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);
        assertThrows(IndexOutOfBoundsException.class, () -> result.getDistanceTo(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> result.isReachable(2));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getPathTo(2));
    }

    @Test
    void accumulatesDistancesBeyondIntRange() {
        WeightedGraph graph = graph(4, true, new int[][]{
                {0, 1, Integer.MAX_VALUE},
                {1, 2, Integer.MAX_VALUE},
                {2, 3, Integer.MAX_VALUE}
        });
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(3L * Integer.MAX_VALUE, result.getDistanceTo(3));
        assertValidPath(graph, result, 3);
    }

    @Test
    void leavesGraphUnchangedAndReturnsImmutableResults() {
        WeightedGraph graph = graph(3, false, new int[][]{{0, 1, 2}, {1, 2, 3}});
        List<Edge> originalEdges = List.copyOf(graph.getEdges());
        List<List<Edge>> originalAdjacency = graph.getAdjacencyLists();
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);

        assertEquals(originalEdges, graph.getEdges());
        assertEquals(originalAdjacency, graph.getAdjacencyLists());
        assertThrows(UnsupportedOperationException.class,
                () -> result.getDistances().add(9L));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getPathTo(2).add(9));
    }

    @Test
    void handlesEmptyGraphSourceValidation() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.findShortestPaths(new WeightedGraph(0, true), 0));
    }

    private static void assertValidPath(WeightedGraph graph, ShortestPathResult result,
                                        int target) {
        List<Integer> path = result.getPathTo(target);
        assertEquals(result.getSource(), path.get(0));
        assertEquals(target, path.get(path.size() - 1));
        Map<Long, Integer> minimumEdgeWeight = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            putMinimum(minimumEdgeWeight, edge.getSource(), edge.getDestination(), edge.getWeight());
            if (!graph.isDirected()) {
                putMinimum(minimumEdgeWeight, edge.getDestination(), edge.getSource(),
                        edge.getWeight());
            }
        }
        long weight = 0;
        for (int index = 0; index + 1 < path.size(); index++) {
            Integer edgeWeight = minimumEdgeWeight.get(pair(path.get(index), path.get(index + 1)));
            assertTrue(edgeWeight != null, "path contains a missing directed edge");
            weight += edgeWeight;
        }
        assertEquals(result.getDistanceTo(target), weight);
    }

    private static void putMinimum(Map<Long, Integer> weights, int source, int destination,
                                   int weight) {
        weights.merge(pair(source, destination), weight, Math::min);
    }

    private static long pair(int source, int destination) {
        return ((long) source << Integer.SIZE) | (destination & 0xffffffffL);
    }

    private static WeightedGraph graph(int vertexCount, boolean directed, int[][] edges) {
        WeightedGraph graph = new WeightedGraph(vertexCount, directed);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1], edge[2]);
        }
        return graph;
    }
}
