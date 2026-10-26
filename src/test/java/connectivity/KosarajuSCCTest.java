package connectivity;

import graph.Edge;
import graph.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KosarajuSCCTest {
    @Test
    void findsOneComponentWhenEveryVertexIsMutuallyReachable() {
        WeightedGraph graph = graph(4, new int[][]{
                {0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 1}
        });

        assertPartition(graph, Set.of(Set.of(0, 1, 2, 3)));
    }

    @Test
    void identifiesDirectedCycleAsOneComponent() {
        WeightedGraph graph = graph(3, new int[][]{{0, 1}, {1, 2}, {2, 0}});

        assertPartition(graph, Set.of(Set.of(0, 1, 2)));
    }

    @Test
    void findsMultipleStronglyConnectedComponentsWithOneWayEdges() {
        WeightedGraph graph = graph(6, new int[][]{
                {0, 1}, {1, 2}, {2, 0},
                {3, 4}, {4, 3},
                {2, 3}, {4, 5}
        });

        assertPartition(graph, Set.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5)));
    }

    @Test
    void treatsIsolatedVerticesAndVerticesWithoutEdgesAsSingletonComponents() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);

        assertPartition(graph, Set.of(Set.of(0), Set.of(1), Set.of(2), Set.of(3)));
    }

    @Test
    void handlesEmptyGraph() {
        assertEquals(List.of(),
                KosarajuSCC.findStronglyConnectedComponents(new WeightedGraph(0, true)));
    }

    @Test
    void handlesDisconnectedDirectedComponents() {
        WeightedGraph graph = graph(7, new int[][]{
                {0, 1}, {1, 0}, {2, 3}, {3, 4}, {4, 2}
        });

        assertPartition(graph, Set.of(
                Set.of(0, 1), Set.of(2, 3, 4), Set.of(5), Set.of(6)));
    }

    @Test
    void rejectsUndirectedGraph() {
        assertThrows(IllegalArgumentException.class, () ->
                KosarajuSCC.findStronglyConnectedComponents(new WeightedGraph(2, false)));
    }

    @Test
    void everyVertexAppearsExactlyOnce() {
        WeightedGraph graph = graph(5, new int[][]{{0, 1}, {1, 0}, {2, 3}});

        List<List<Integer>> components =
                KosarajuSCC.findStronglyConnectedComponents(graph);
        List<Integer> allVertices = components.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertEquals(graph.getVertexCount(), allVertices.size());
        assertEquals(graph.getVertexCount(), new HashSet<>(allVertices).size());
        assertEquals(Set.of(0, 1, 2, 3, 4), new HashSet<>(allVertices));
    }

    @Test
    void leavesGraphUnchangedAndReturnsImmutableComponents() {
        WeightedGraph graph = graph(3, new int[][]{{0, 1}, {1, 2}, {2, 0}});
        List<Edge> originalEdges = List.copyOf(graph.getEdges());
        List<List<Edge>> originalAdjacency = graph.getAdjacencyLists();

        List<List<Integer>> components =
                KosarajuSCC.findStronglyConnectedComponents(graph);

        assertEquals(originalEdges, graph.getEdges());
        assertEquals(originalAdjacency, graph.getAdjacencyLists());
        assertThrows(UnsupportedOperationException.class, () -> components.add(List.of(9)));
        assertThrows(UnsupportedOperationException.class, () -> components.get(0).add(9));
    }

    private static void assertPartition(WeightedGraph graph, Set<Set<Integer>> expected) {
        List<List<Integer>> components =
                KosarajuSCC.findStronglyConnectedComponents(graph);
        Set<Set<Integer>> actual = components.stream()
                .map(HashSet::new)
                .map(Set::copyOf)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);

        List<Integer> allVertices = components.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        assertEquals(graph.getVertexCount(), allVertices.size());
        assertEquals(graph.getVertexCount(), new HashSet<>(allVertices).size());
    }

    private static WeightedGraph graph(int vertexCount, int[][] edges) {
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1], 1);
        }
        return graph;
    }
}
