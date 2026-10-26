package connectivity;

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

class TopologicalSortTest {
    @Test
    void ordersASimpleDag() {
        WeightedGraph graph = graph(4, new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}});

        assertValidOrdering(graph, TopologicalSort.sort(graph).getOrdering());
    }

    @Test
    void acceptsAnyValidOrderingWhenSeveralExist() {
        WeightedGraph graph = graph(3, new int[][]{{0, 2}, {1, 2}});

        TopologicalSortResult result = TopologicalSort.sort(graph);

        assertFalse(result.hasCycle());
        assertValidOrdering(graph, result.getOrdering());
        assertTrue(result.getOrdering().indexOf(0) < result.getOrdering().indexOf(2));
        assertTrue(result.getOrdering().indexOf(1) < result.getOrdering().indexOf(2));
    }

    @Test
    void handlesDisconnectedDagAndIsolatedVertices() {
        WeightedGraph graph = graph(6, new int[][]{{0, 1}, {3, 4}});

        assertValidOrdering(graph, TopologicalSort.sort(graph).getOrdering());
    }

    @Test
    void handlesEmptyAndSingleVertexGraphs() {
        TopologicalSortResult empty = TopologicalSort.sort(new WeightedGraph(0, true));
        TopologicalSortResult single = TopologicalSort.sort(new WeightedGraph(1, true));

        assertFalse(empty.hasCycle());
        assertTrue(empty.getOrdering().isEmpty());
        assertFalse(single.hasCycle());
        assertEquals(List.of(0), single.getOrdering());
    }

    @Test
    void detectsMultiVertexCycleAndDoesNotReturnPartialOrdering() {
        WeightedGraph graph = graph(4, new int[][]{{0, 1}, {1, 2}, {2, 1}, {2, 3}});

        TopologicalSortResult result = TopologicalSort.sort(graph);

        assertTrue(result.hasCycle());
        assertTrue(result.getOrdering().isEmpty());
    }

    @Test
    void detectsSelfLoop() {
        WeightedGraph graph = graph(1, new int[][]{{0, 0}});

        assertTrue(TopologicalSort.sort(graph).hasCycle());
    }

    @Test
    void countsDuplicateEdgesIndividually() {
        WeightedGraph graph = graph(2, new int[][]{{0, 1}, {0, 1}, {0, 1}});

        assertValidOrdering(graph, TopologicalSort.sort(graph).getOrdering());
    }

    @Test
    void rejectsUndirectedGraph() {
        assertThrows(IllegalArgumentException.class,
                () -> TopologicalSort.sort(new WeightedGraph(2, false)));
    }

    @Test
    void leavesGraphUnchangedAndReturnsImmutableOrdering() {
        WeightedGraph graph = graph(3, new int[][]{{0, 1}, {1, 2}});
        List<Edge> originalEdges = List.copyOf(graph.getEdges());
        List<List<Edge>> originalAdjacency = graph.getAdjacencyLists();
        TopologicalSortResult result = TopologicalSort.sort(graph);

        assertEquals(originalEdges, graph.getEdges());
        assertEquals(originalAdjacency, graph.getAdjacencyLists());
        assertThrows(UnsupportedOperationException.class,
                () -> result.getOrdering().add(9));
    }

    private static void assertValidOrdering(WeightedGraph graph, List<Integer> ordering) {
        assertFalse(TopologicalSort.sort(graph).hasCycle());
        assertEquals(graph.getVertexCount(), ordering.size());
        Map<Integer, Integer> positions = new HashMap<>();
        for (int index = 0; index < ordering.size(); index++) {
            assertTrue(positions.put(ordering.get(index), index) == null);
        }
        assertEquals(graph.getVertexCount(), positions.size());
        for (Edge edge : graph.getEdges()) {
            assertTrue(positions.get(edge.getSource()) < positions.get(edge.getDestination()));
        }
    }

    private static WeightedGraph graph(int vertexCount, int[][] edges) {
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1], 1);
        }
        return graph;
    }
}
