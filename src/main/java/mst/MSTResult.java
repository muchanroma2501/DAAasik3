package mst;

import graph.Edge;

import java.util.List;

/**
 * Immutable result of a minimum spanning tree or forest computation.
 *
 * <p>{@code connected} is true exactly when the input graph has a spanning
 * tree. A one-vertex graph is connected and has an empty spanning tree. A
 * zero-vertex graph is considered disconnected because no spanning tree
 * exists for it under this API's policy.</p>
 */
public final class MSTResult {
    private final List<Edge> edges;
    private final long totalWeight;
    private final boolean connected;

    /**
     * Creates an immutable result from selected edges.
     *
     * @param edges selected tree or forest edges
     * @param totalWeight sum of selected edge weights
     * @param connected whether a full spanning tree exists in the input graph
     */
    public MSTResult(List<Edge> edges, long totalWeight, boolean connected) {
        this.edges = List.copyOf(edges);
        this.totalWeight = totalWeight;
        this.connected = connected;
    }

    /**
     * Returns the selected edges as an immutable list.
     *
     * @return selected edges
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Returns the sum of selected edge weights.
     *
     * @return total weight
     */
    public long getTotalWeight() {
        return totalWeight;
    }

    /**
     * Returns whether the input graph has a full spanning tree.
     *
     * @return true for a connected graph with at least one vertex; false for a
     *         disconnected graph or a graph with zero vertices
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the number of selected edges.
     *
     * @return selected edge count
     */
    public int getEdgeCount() {
        return edges.size();
    }

    @Override
    public String toString() {
        return "MSTResult{edges=" + edges
                + ", totalWeight=" + totalWeight
                + ", connected=" + connected
                + '}';
    }
}
