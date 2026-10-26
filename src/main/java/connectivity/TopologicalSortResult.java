package connectivity;

import java.util.List;

/**
 * Immutable result of a topological sort.
 *
 * <p>The ordering is empty when the graph contains a directed cycle, so a
 * partial traversal cannot be mistaken for a valid topological ordering.</p>
 */
public final class TopologicalSortResult {
    private final List<Integer> ordering;
    private final boolean hasCycle;

    /**
     * Creates a topological-sort result.
     *
     * @param ordering complete ordering, or an empty list when a cycle exists
     * @param hasCycle whether the graph contains a directed cycle
     */
    TopologicalSortResult(List<Integer> ordering, boolean hasCycle) {
        this.ordering = List.copyOf(ordering);
        this.hasCycle = hasCycle;
    }

    /**
     * Returns the complete ordering, or an empty list when a cycle exists.
     *
     * @return immutable vertex ordering
     */
    public List<Integer> getOrdering() {
        return ordering;
    }

    /**
     * Returns whether the input graph contains a directed cycle.
     *
     * @return true when the graph is cyclic
     */
    public boolean hasCycle() {
        return hasCycle;
    }
}
