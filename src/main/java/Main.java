import graph.Edge;
import graph.WeightedGraph;

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
    }
}
