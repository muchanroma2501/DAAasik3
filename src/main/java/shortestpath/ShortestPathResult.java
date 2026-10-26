package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable shortest-path distances and predecessor data from one source.
 *
 * <p>{@link Long#MAX_VALUE} represents an unreachable vertex.</p>
 */
public final class ShortestPathResult {
    private final int source;
    private final long[] distances;
    private final int[] predecessors;

    /**
     * Creates an immutable shortest-path result.
     *
     * @param source source vertex
     * @param distances shortest distances, with {@link Long#MAX_VALUE} for
     *                  unreachable vertices
     * @param predecessors predecessor vertex for each reachable vertex
     */
    ShortestPathResult(int source, long[] distances, int[] predecessors) {
        this.source = source;
        this.distances = distances.clone();
        this.predecessors = predecessors.clone();
    }

    /**
     * Returns the source vertex used for the computation.
     *
     * @return source vertex ID
     */
    public int getSource() {
        return source;
    }

    /**
     * Returns the shortest distance to a target.
     *
     * @param target target vertex ID
     * @return shortest distance, or {@link Long#MAX_VALUE} if unreachable
     * @throws IndexOutOfBoundsException if the target is outside this result
     */
    public long getDistanceTo(int target) {
        validateTarget(target);
        return distances[target];
    }

    /**
     * Returns whether a target is reachable from the source.
     *
     * @param target target vertex ID
     * @return true if a path exists
     * @throws IndexOutOfBoundsException if the target is outside this result
     */
    public boolean isReachable(int target) {
        validateTarget(target);
        return distances[target] != Long.MAX_VALUE;
    }

    /**
     * Returns the shortest path from the source to a target.
     *
     * @param target target vertex ID
     * @return immutable source-to-target path, or an empty list if unreachable
     * @throws IndexOutOfBoundsException if the target is outside this result
     */
    public List<Integer> getPathTo(int target) {
        validateTarget(target);
        if (distances[target] == Long.MAX_VALUE) {
            return List.of();
        }

        List<Integer> reversedPath = new ArrayList<>();
        int vertex = target;
        while (vertex != source) {
            reversedPath.add(vertex);
            vertex = predecessors[vertex];
            if (vertex < 0) {
                throw new IllegalStateException("Reachable vertex has no predecessor path");
            }
        }
        reversedPath.add(source);
        Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }

    /**
     * Returns all shortest distances in vertex-ID order.
     *
     * @return immutable distances; {@link Long#MAX_VALUE} denotes unreachable
     */
    public List<Long> getDistances() {
        List<Long> result = new ArrayList<>(distances.length);
        for (long distance : distances) {
            result.add(distance);
        }
        return List.copyOf(result);
    }

    private void validateTarget(int target) {
        if (target < 0 || target >= distances.length) {
            throw new IndexOutOfBoundsException(
                    "Target vertex " + target + " is outside [0, " + distances.length + ")");
        }
    }
}
