import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;

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
}
