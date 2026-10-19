package graph;

import java.util.Objects;

/**
 * An immutable weighted edge between two vertex IDs.
 *
 * <p>Equality is ordered: source and destination are compared in their given
 * order, along with the weight. Therefore, {@code 1 -> 2} is not equal to
 * {@code 2 -> 1}. Edges can have any integer weight, including negative
 * weights; algorithms that require a weight restriction must validate it.</p>
 */
public final class Edge {
    private final int source;
    private final int destination;
    private final int weight;

    /**
     * Creates an edge with the given endpoints and integer weight.
     *
     * @param source the source vertex ID
     * @param destination the destination vertex ID
     * @param weight the edge weight
     */
    public Edge(int source, int destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Edge)) {
            return false;
        }
        Edge edge = (Edge) other;
        return source == edge.source
                && destination == edge.destination
                && weight == edge.weight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, weight);
    }

    @Override
    public String toString() {
        return source + " -> " + destination + " (weight " + weight + ")";
    }
}
