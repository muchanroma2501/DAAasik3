package mst;

import graph.Edge;
import graph.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MSTComparisonTest {
    @Test
    void algorithmsAgreeOnDeterministicConnectedGraphs() {
        List<WeightedGraph> graphs = List.of(
                graph(4, new int[][]{{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {3, 0, 1}, {0, 2, 1}}),
                graph(5, new int[][]{{0, 1, -2}, {0, 2, 6}, {1, 2, 3}, {1, 3, 5},
                        {2, 3, 1}, {2, 4, 4}, {3, 4, 2}}),
                graph(3, new int[][]{{0, 1, 10}, {0, 2, 10}, {1, 2, 10}})
        );

        for (WeightedGraph graph : graphs) {
            MSTResult kruskal = Kruskal.findMst(graph);
            MSTResult prim = Prim.findMst(graph);

            assertEquals(kruskal.getTotalWeight(), prim.getTotalWeight());
            assertEquals(graph.getVertexCount() - 1, kruskal.getEdgeCount());
            assertEquals(graph.getVertexCount() - 1, prim.getEdgeCount());
            assertTrue(kruskal.isConnected());
            assertTrue(prim.isConnected());
            assertInputEdges(graph, kruskal);
            assertInputEdges(graph, prim);
        }
    }

    private void assertInputEdges(WeightedGraph graph, MSTResult result) {
        for (Edge edge : result.getEdges()) {
            assertTrue(graph.getEdges().contains(edge));
        }
    }

    private static WeightedGraph graph(int vertexCount, int[][] edges) {
        WeightedGraph graph = new WeightedGraph(vertexCount, false);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1], edge[2]);
        }
        return graph;
    }
}
