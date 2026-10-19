package graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedGraphTest {
    @Test
    void allowsAnEmptyGraph() {
        WeightedGraph graph = new WeightedGraph(0, false);

        assertEquals(0, graph.getVertexCount());
        assertTrue(graph.getEdges().isEmpty());
        assertTrue(graph.getAdjacencyLists().isEmpty());
    }

    @Test
    void allowsASingleVertexGraph() {
        WeightedGraph graph = new WeightedGraph(1, false);

        assertEquals(1, graph.getVertexCount());
        assertTrue(graph.getAdjacencyList(0).isEmpty());
    }

    @Test
    void rejectsNegativeVertexCount() {
        assertThrows(IllegalArgumentException.class, () -> new WeightedGraph(-1, false));
    }

    @Test
    void addsAnUndirectedEdgeOnceAndMakesItAccessibleAtBothEndpoints() {
        WeightedGraph graph = new WeightedGraph(2, false);
        Edge edge = new Edge(0, 1, 8);

        graph.addEdge(0, 1, 8);

        assertEquals(List.of(edge), graph.getEdges());
        assertEquals(List.of(edge), graph.getAdjacencyList(0));
        assertEquals(List.of(edge), graph.getAdjacencyList(1));
    }

    @Test
    void addsADirectedEdgeOnlyToItsSourceAdjacencyList() {
        WeightedGraph graph = new WeightedGraph(2, true);
        Edge edge = new Edge(0, 1, 4);

        graph.addEdge(0, 1, 4);

        assertEquals(List.of(edge), graph.getEdges());
        assertEquals(List.of(edge), graph.getAdjacencyList(0));
        assertTrue(graph.getAdjacencyList(1).isEmpty());
    }

    @Test
    void permitsDuplicateEdgesAndSelfLoops() {
        WeightedGraph graph = new WeightedGraph(2, false);
        Edge edge = new Edge(0, 1, 6);
        Edge selfLoop = new Edge(1, 1, 2);

        graph.addEdge(0, 1, 6);
        graph.addEdge(0, 1, 6);
        graph.addEdge(1, 1, 2);

        assertEquals(List.of(edge, edge, selfLoop), graph.getEdges());
        assertEquals(List.of(edge, edge, selfLoop), graph.getAdjacencyList(1));
    }

    @Test
    void rejectsInvalidVertexIndices() {
        WeightedGraph graph = new WeightedGraph(2, false);

        assertThrows(IndexOutOfBoundsException.class, () -> graph.addEdge(-1, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> graph.addEdge(0, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> graph.getAdjacencyList(2));
    }

    @Test
    void returnedCollectionsCannotMutateGraphState() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 5);

        assertThrows(UnsupportedOperationException.class,
                () -> graph.getEdges().add(new Edge(0, 1, 7)));
        assertThrows(UnsupportedOperationException.class,
                () -> graph.getAdjacencyList(0).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> graph.getAdjacencyLists().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> graph.getAdjacencyLists().get(0).clear());
        assertEquals(1, graph.getEdges().size());
        assertEquals(1, graph.getAdjacencyList(0).size());
    }

    @Test
    void edgeEqualityAndHashCodeUseOrderedEndpointsAndWeight() {
        Edge edge = new Edge(0, 1, -3);
        Edge equalEdge = new Edge(0, 1, -3);
        Edge reversedEdge = new Edge(1, 0, -3);
        Edge differentWeight = new Edge(0, 1, 3);

        assertEquals(edge, equalEdge);
        assertEquals(edge.hashCode(), equalEdge.hashCode());
        assertNotEquals(edge, reversedEdge);
        assertNotEquals(edge, differentWeight);
        assertFalse(edge.equals(null));
    }
}
