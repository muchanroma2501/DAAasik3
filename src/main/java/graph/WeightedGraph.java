package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A weighted directed or undirected graph with vertex IDs in {@code [0, V)}.
 *
 * <p>Zero vertices are allowed and produce an empty graph. A one-vertex graph
 * is also allowed and can contain self-loops. Duplicate edges and self-loops
 * are not rejected. Each call to {@link #addEdge(int, int, int)} adds exactly
 * one edge-list entry. An undirected edge is also placed in both endpoint
 * adjacency lists; an undirected self-loop is listed once in its sole
 * adjacency list.</p>
 */
public class WeightedGraph {
    private final int vertexCount;
    private final boolean directed;
    private final List<Edge> edges;
    private final List<List<Edge>> adjacencyLists;

    /**
     * Creates an empty graph.
     *
     * @param vertexCount number of vertices; must be zero or greater
     * @param directed whether the graph's edges are directed
     * @throws IllegalArgumentException if {@code vertexCount} is negative
     */
    public WeightedGraph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Vertex count cannot be negative: " + vertexCount);
        }

        this.vertexCount = vertexCount;
        this.directed = directed;
        this.edges = new ArrayList<>();
        this.adjacencyLists = new ArrayList<>(vertexCount);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacencyLists.add(new ArrayList<>());
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public boolean isDirected() {
        return directed;
    }

    /**
     * Returns an unmodifiable view of the logical edges, with each added edge
     * appearing once, including in an undirected graph.
     */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /**
     * Returns an unmodifiable view of the edges adjacent to a vertex.
     *
     * <p>For an undirected edge, the same logical edge is accessible from both
     * endpoint lists. Edge endpoint order remains the order used when it was
     * added.</p>
     *
     * @param vertex vertex whose adjacency list is requested
     * @return an unmodifiable adjacency list
     * @throws IndexOutOfBoundsException if the vertex is not in this graph
     */
    public List<Edge> getAdjacencyList(int vertex) {
        validateVertex(vertex);
        return Collections.unmodifiableList(adjacencyLists.get(vertex));
    }

    /**
     * Returns an unmodifiable outer list containing unmodifiable adjacency
     * lists.
     */
    public List<List<Edge>> getAdjacencyLists() {
        List<List<Edge>> readOnlyLists = new ArrayList<>(vertexCount);
        for (List<Edge> adjacencyList : adjacencyLists) {
            readOnlyLists.add(Collections.unmodifiableList(adjacencyList));
        }
        return Collections.unmodifiableList(readOnlyLists);
    }

    /**
     * Adds an edge. Duplicate edges and self-loops are permitted.
     *
     * @throws IndexOutOfBoundsException if either endpoint is not in this graph
     */
    public void addEdge(int source, int destination, int weight) {
        validateVertex(source);
        validateVertex(destination);

        Edge edge = new Edge(source, destination, weight);
        edges.add(edge);
        adjacencyLists.get(source).add(edge);
        if (!directed && source != destination) {
            adjacencyLists.get(destination).add(edge);
        }
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IndexOutOfBoundsException(
                    "Vertex index " + vertex + " is outside [0, " + vertexCount + ")");
        }
    }

    @Override
    public String toString() {
        return "WeightedGraph{vertexCount=" + vertexCount
                + ", directed=" + directed
                + ", edges=" + edges
                + ", adjacencyLists=" + getAdjacencyLists()
                + '}';
    }
}
