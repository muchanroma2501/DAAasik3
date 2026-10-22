package mst;

import graph.Edge;
import graph.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimTest {
    @Test
    void returnsSameWeightAsKruskalForKnownGraph() {
        WeightedGraph graph = knownGraph();

        MSTResult result = Prim.findMst(graph);

        assertEquals(6, result.getTotalWeight());
        assertEquals(Kruskal.findMst(graph).getTotalWeight(), result.getTotalWeight());
        assertTrue(result.isConnected());
    }

    @Test
    void selectsExactlyVertexCountMinusOneEdgesForConnectedGraphs() {
        WeightedGraph graph = knownGraph();

        assertEquals(graph.getVertexCount() - 1, Prim.findMst(graph).getEdgeCount());
    }

    @Test
    void supportsNegativeAndDuplicateWeights() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, -4);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 2, 2);

        MSTResult result = Prim.findMst(graph);

        assertEquals(-2, result.getTotalWeight());
        assertEquals(2, result.getEdgeCount());
        assertTrue(result.isConnected());
    }

    @Test
    void sumsWeightsUsingLong() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);

        assertEquals(2L * Integer.MAX_VALUE, Prim.findMst(graph).getTotalWeight());
    }

    @Test
    void excludesSelfLoops() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 0, -10);
        graph.addEdge(0, 1, 4);

        MSTResult result = Prim.findMst(graph);

        assertEquals(1, result.getEdgeCount());
        assertEquals(4, result.getTotalWeight());
    }

    @Test
    void returnsForestIncludingIsolatedVerticesForDisconnectedGraphs() {
        WeightedGraph graph = new WeightedGraph(5, false);
        graph.addEdge(0, 1, 2);
        graph.addEdge(2, 3, 1);

        MSTResult result = Prim.findMst(graph, 2);

        assertEquals(2, result.getEdgeCount());
        assertEquals(3, result.getTotalWeight());
        assertFalse(result.isConnected());
    }

    @Test
    void handlesSingleAndZeroVertexGraphs() {
        MSTResult single = Prim.findMst(new WeightedGraph(1, false));
        MSTResult empty = Prim.findMst(new WeightedGraph(0, false));

        assertTrue(single.isConnected());
        assertEquals(0, single.getEdgeCount());
        assertFalse(empty.isConnected());
        assertEquals(0, empty.getEdgeCount());
    }

    @Test
    void rejectsDirectedGraphsAndInvalidRequestedSources() {
        assertThrows(IllegalArgumentException.class,
                () -> Prim.findMst(new WeightedGraph(2, true)));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Prim.findMst(new WeightedGraph(2, false), 2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Prim.findMst(new WeightedGraph(0, false), 0));
    }

    @Test
    void leavesInputGraphUnchangedAndReturnsEdgesFromThatGraph() {
        WeightedGraph graph = knownGraph();
        List<Edge> originalEdges = List.copyOf(graph.getEdges());

        MSTResult result = Prim.findMst(graph);

        assertEquals(originalEdges, graph.getEdges());
        assertTrue(originalEdges.containsAll(result.getEdges()));
    }

    @Test
    void resultEdgeListIsReadOnly() {
        MSTResult result = Prim.findMst(knownGraph());

        assertThrows(UnsupportedOperationException.class,
                () -> result.getEdges().add(new Edge(0, 1, 1)));
    }

    private WeightedGraph knownGraph() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 5);
        return graph;
    }
}
