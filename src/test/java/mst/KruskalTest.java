package mst;

import graph.Edge;
import graph.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KruskalTest {
    @Test
    void findsKnownMinimumSpanningTree() {
        WeightedGraph graph = knownGraph();

        MSTResult result = Kruskal.findMst(graph);

        assertEquals(6, result.getTotalWeight());
        assertEquals(3, result.getEdgeCount());
        assertTrue(result.isConnected());
    }

    @Test
    void selectsExactlyVertexCountMinusOneEdgesForConnectedGraphs() {
        WeightedGraph graph = knownGraph();

        assertEquals(graph.getVertexCount() - 1, Kruskal.findMst(graph).getEdgeCount());
    }

    @Test
    void supportsNegativeAndDuplicateWeights() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, -4);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 1, 2);

        MSTResult result = Kruskal.findMst(graph);

        assertEquals(-2, result.getTotalWeight());
        assertEquals(2, result.getEdgeCount());
        assertTrue(result.isConnected());
    }

    @Test
    void sumsWeightsUsingLong() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);

        assertEquals(2L * Integer.MAX_VALUE, Kruskal.findMst(graph).getTotalWeight());
    }

    @Test
    void excludesCycleEdges() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);

        MSTResult result = Kruskal.findMst(graph);

        assertEquals(2, result.getEdgeCount());
        assertEquals(3, result.getTotalWeight());
    }

    @Test
    void returnsMinimumSpanningForestForDisconnectedGraphs() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 2);
        graph.addEdge(2, 3, 1);

        MSTResult result = Kruskal.findMst(graph);

        assertEquals(2, result.getEdgeCount());
        assertEquals(3, result.getTotalWeight());
        assertFalse(result.isConnected());
    }

    @Test
    void handlesSingleAndZeroVertexGraphs() {
        MSTResult single = Kruskal.findMst(new WeightedGraph(1, false));
        MSTResult empty = Kruskal.findMst(new WeightedGraph(0, false));

        assertTrue(single.isConnected());
        assertEquals(0, single.getEdgeCount());
        assertEquals(0, single.getTotalWeight());
        assertFalse(empty.isConnected());
        assertEquals(0, empty.getEdgeCount());
        assertEquals(0, empty.getTotalWeight());
    }

    @Test
    void rejectsDirectedGraphs() {
        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.findMst(new WeightedGraph(2, true)));
    }

    @Test
    void doesNotSelectSelfLoopsOrModifyTheInputGraph() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 0, -10);
        graph.addEdge(0, 1, 4);
        List<Edge> originalEdges = List.copyOf(graph.getEdges());

        MSTResult result = Kruskal.findMst(graph);

        assertEquals(1, result.getEdgeCount());
        assertEquals(4, result.getTotalWeight());
        assertEquals(originalEdges, graph.getEdges());
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
