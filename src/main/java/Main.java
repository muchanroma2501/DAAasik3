import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import connectivity.TopologicalSortResult;
import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

/**
 * Prints a small deterministic example of the Day 1 graph model.
 */
public class Main {
    public static void main(String[] args) {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 3, 7);

        System.out.println("Vertices: " + graph.getVertexCount());
        System.out.println("Graph type: " + (graph.isDirected() ? "directed" : "undirected"));
        System.out.println("Edge list (each logical edge once):");
        for (Edge edge : graph.getEdges()) {
            System.out.println("  " + edge);
        }

        System.out.println("Adjacency lists:");
        for (int vertex = 0; vertex < graph.getVertexCount(); vertex++) {
            System.out.println("  " + vertex + ": " + graph.getAdjacencyList(vertex));
        }

        demonstrateMinimumSpanningTrees();
        demonstrateConnectivityAndShortestPaths();
    }

    private static void demonstrateMinimumSpanningTrees() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 5);

        MSTResult kruskalResult = Kruskal.findMst(graph);
        MSTResult primResult = Prim.findMst(graph);

        System.out.println();
        System.out.println("Minimum spanning tree demonstration:");
        System.out.println("Kruskal edges: " + kruskalResult.getEdges());
        System.out.println("Kruskal total weight: " + kruskalResult.getTotalWeight());
        System.out.println("Prim edges: " + primResult.getEdges());
        System.out.println("Prim total weight: " + primResult.getTotalWeight());
        System.out.println("Graph is connected: " + kruskalResult.isConnected());
        System.out.println("Algorithms agree on total weight: "
                + (kruskalResult.getTotalWeight() == primResult.getTotalWeight()));
    }

    private static void demonstrateConnectivityAndShortestPaths() {
        WeightedGraph dag = new WeightedGraph(4, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        TopologicalSortResult ordering = TopologicalSort.sort(dag);

        WeightedGraph cyclic = new WeightedGraph(2, true);
        cyclic.addEdge(0, 1, 1);
        cyclic.addEdge(1, 0, 1);

        WeightedGraph componentGraph = new WeightedGraph(4, true);
        componentGraph.addEdge(0, 1, 1);
        componentGraph.addEdge(1, 0, 1);
        componentGraph.addEdge(1, 2, 1);
        componentGraph.addEdge(2, 3, 1);
        componentGraph.addEdge(3, 2, 1);

        WeightedGraph routes = new WeightedGraph(5, false);
        routes.addEdge(0, 1, 4);
        routes.addEdge(0, 2, 2);
        routes.addEdge(2, 1, 1);
        routes.addEdge(1, 3, 3);
        routes.addEdge(2, 3, 7);
        routes.addEdge(3, 4, 2);
        ShortestPathResult paths = Dijkstra.findShortestPaths(routes, 0);

        System.out.println();
        System.out.println("Topological ordering: " + ordering.getOrdering());
        System.out.println("DAG contains cycle: " + ordering.hasCycle());
        System.out.println("Separate graph contains cycle: "
                + TopologicalSort.sort(cyclic).hasCycle());
        System.out.println("Strongly connected components: "
                + KosarajuSCC.findStronglyConnectedComponents(componentGraph));
        System.out.println("Dijkstra distances from 0: " + paths.getDistances());
        System.out.println("Shortest path from 0 to 4: " + paths.getPathTo(4));
    }
}
